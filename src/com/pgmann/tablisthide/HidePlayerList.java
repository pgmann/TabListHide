/*
 * TabListHide - Hide or show players in the tab player list
 * Copyright (C) 2017 pgmann
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.pgmann.tablisthide;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.BukkitUnwrapper;
import com.comphenix.protocol.reflect.FieldUtils;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;

public class HidePlayerList {

	// ProtocolLib
	private PacketListener overrideListener;
	private ProtocolManager manager;

	// Players to hide
	private Set<String> hiddenPlayers = new HashSet<String>();
	private Set<String> fixedPlayers = new HashSet<String>();

	// To get the ping
	private Field pingField;

	/**
	 * Start the player list hook
	 * 
	 * @param plugin the owner plugin
	 */
	protected HidePlayerList(final TabListHide p) {
		this.overrideListener = new PacketAdapter(p, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
			@Override
			public void onPacketSending(PacketEvent event) {
				// Only alter ADD_PLAYER actions
				if (event.getPacket().getPlayerInfoAction().read(0) != PlayerInfoAction.ADD_PLAYER) return;

				// Get packet data
				List<PlayerInfoData> playerInfoDataList = event.getPacket().getPlayerInfoDataLists().read(0);
				PlayerInfoData playerInfoData = playerInfoDataList.get(0);

				// Check the packet data is valid
				if (playerInfoData == null || playerInfoData.getProfile() == null || Bukkit.getPlayer(playerInfoData.getProfile().getUUID()) == null) {
					return; // Unknown Player
				}

				// Check if the player needs hidden
				String name = playerInfoData.getProfile().getName();
				if (hiddenPlayers.contains(name)) {
					final Player player = Bukkit.getPlayer(name);

					// Ignore "fixed" players - those in spectator mode, who need to be shown in their own tab list
					if(player.equals(event.getPlayer()) && fixedPlayers.contains(player.getName())) return;
					
					// Must allow the player to spawn before removing from player list - hide display name initially instead
					playerInfoDataList.set(0, new PlayerInfoData(playerInfoData.getProfile(), getPlayerPing(player), playerInfoData.getGameMode(), WrappedChatComponent.fromText("")));

					// Update packet
					event.getPacket().getPlayerInfoDataLists().write(0, playerInfoDataList);
					
					// Hide the player completely after 10 ticks
					new BukkitRunnable() {
						@Override
						public void run() {
							hidePlayer(player);
						}
					}.runTaskLater(p, 10);
				}
			}
		};
		this.manager = ProtocolLibrary.getProtocolManager();
	}

	/**
	 * Start the hook
	 */
	protected void register() {
		manager.addPacketListener(overrideListener);
	}

	/**
	 * Hide a player from the list
	 * 
	 * @param player the player to hide from the list
	 * @return if anything happened (ie. player was visible and is now hidden)
	 */
	protected boolean hidePlayer(Player player) {
		// Send the packet to all players
		final ArrayList<Player> targets = new ArrayList<Player>();
		targets.addAll(Bukkit.getOnlinePlayers());

		// The target must be in their own tab list or they can't no-clip/use spectator tools
		if(player.getGameMode() == GameMode.SPECTATOR) targets.remove(player);

		// Add to list of hidden players
		String name = player.getName();
		boolean success = hiddenPlayers.add(name);

		// Send packet to targets
		sendInfoPacket(player, false, targets);
		return success;
	}

	/**
	 * Show a player on the list
	 * 
	 * @param player the player to show in the list
	 * @return if anything happened (ie. player was hidden and is now visible)
	 */
	protected boolean showPlayer(Player player) {
		String name = player.getName();
		boolean success = hiddenPlayers.remove(name);

		sendInfoPacket(player, true);
		return success;
	}

	/**
	 * Fixes a bug where Spectators (gamemode 3) can't no-clip or use tools.
	 * - Visible players are ignored.
	 * - Adds the player to their own tab list if they're spectator mode.
	 * - If the player is not in spectator mode, they will be removed from the tab list.
	 * 
	 * @param player the player to fix
	 */
	protected void fixPlayer(Player player) {
		// Ignore visible players
		if(isVisible(player)) return;

		// Packet will be sent to the target player only
		ArrayList<Player> targets = new ArrayList<Player>();
		targets.add(player);
		
		// Ignore the packet in the listener
		fixedPlayers.add(player.getName());

		// Show spectators in their own tab list
		if(player.getGameMode() == GameMode.SPECTATOR) {
			sendInfoPacket(player, true, targets);
		}
		// Hide all other players in all tab lists
		else {
			sendInfoPacket(player, false, targets);
		}

		// Unignore the packet in the listener again
		fixedPlayers.remove(player.getName());
	}

	/**
	 * Determine if a given player is visible in the player list.
	 * 
	 * @param player the player to check.
	 * @return whether the player is visible
	 */
	protected boolean isVisible(Player player) {
		return !hiddenPlayers.contains(player.getName());
	}

	/**
	 * Retrieve the current ping value of a player
	 * 
	 * @param player the player to retrieve
	 * @return the ping value
	 */
	private int getPlayerPing(Player player) {
		BukkitUnwrapper unwrapper = new BukkitUnwrapper();
		Object entity = unwrapper.unwrapItem(player);

		// Next, get the "ping" field
		if (pingField == null) {
			pingField = FuzzyReflection.fromObject(entity).getFieldByName("ping");
		}

		try {
			return (Integer) FieldUtils.readField(pingField, entity);
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return 0;
		}
	}

	/**
	 * Updates the player list for each client after a player has been hidden/shown
	 * 
	 * @param player the player affected
	 * @param visible whether to show or hide the player
	 */
	private void sendInfoPacket(Player player, boolean visible) {
		sendInfoPacket(player, visible, null);
	}

	/**
	 * Updates the player list for each client after a player has been hidden/shown
	 * 
	 * @param player the player affected
	 * @param visible whether to show or hide the player
	 * @param targetOnly only send the packet to the target player
	 */
	private void sendInfoPacket(Player player, boolean visible, List<Player> targets) {
		// BUILD
		WrappedPlayServerPlayerInfo packet = new WrappedPlayServerPlayerInfo();

		// Packet Action
		packet.setAction(visible ? PlayerInfoAction.ADD_PLAYER : PlayerInfoAction.REMOVE_PLAYER);

		// Packet Data
		List<PlayerInfoData> list = packet.getData();
		list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(player), getPlayerPing(player), NativeGameMode.fromBukkit(player.getGameMode()), WrappedChatComponent.fromText(player.getName())));
		packet.setData(list);

		// SEND to targets, or all online players
		for(Player p : targets != null ? targets : Bukkit.getOnlinePlayers()) {
			packet.sendPacket(p);
		}
	}

	/**
	 * Retrieve all hidden players
	 * 
	 * @return the hidden players' names
	 */
	protected Set<String> getHiddenPlayers() {
		return Collections.unmodifiableSet(hiddenPlayers);
	}

	/**
	 * Clean up this hook by removing the listener
	 */
	protected void cleanupAll() {
		if (overrideListener != null) {
			manager.removePacketListener(overrideListener);
			overrideListener = null;
		}
	}
}