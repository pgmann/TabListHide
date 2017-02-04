/*
 *  HidePlayerList - Hide or show players in the player list only (TAB).
 *  Copyright (C) 2012 Kristian S. Stangeland
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the 
 *  GNU General Public License as published by the Free Software Foundation; either version 2 of 
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
 *  See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with this program; 
 *  if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 
 *  02111-1307 USA
 *
 *  Credits:
 *   * Parts of this code was adapted from the Bukkit plugin Orebfuscator by lishid. 
 */

package com.pgmann.tablisthide;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
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

	// To get the ping
	private Field pingField;

	/**
	 * Start the player list hook.
	 * 
	 * @param plugin - owner plugin.
	 */
	public HidePlayerList(final TabListHide p) {
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
					// Must allow the player to spawn before removing from player list - hide display name initially instead
					playerInfoDataList.set(0, new PlayerInfoData(playerInfoData.getProfile(), 1000, playerInfoData.getGameMode(), WrappedChatComponent.fromText("")));
					
					// Hide the player completely after a few ticks
					final Player player = Bukkit.getPlayer(name);
					new BukkitRunnable() {
						@Override
						public void run() {
							sendInfoPacket(player, false);
						}
					}.runTaskLater(p, 10);
				}

				// Update packet
				event.getPacket().getPlayerInfoDataLists().write(0, playerInfoDataList);
			}
		};
		this.manager = ProtocolLibrary.getProtocolManager();
	}

	/**
	 * Start the hook.
	 */
	public void register() {
		manager.addPacketListener(overrideListener);
	}

	/**
	 * Hide a player from the list.
	 * 
	 * @param player - the player to hide from the list.
	 * @return TRUE if the player was not previously hidden, FALSE otherwise.
	 */
	public boolean hidePlayer(Player player) {
		String name = player.getPlayerListName();
		boolean success = hiddenPlayers.add(name);

		sendInfoPacket(player, false);
		return success;
	}

	/**
	 * Show a player on the list.
	 * 
	 * @param player - the player to show on the list.
	 * @return TRUE if the player was previously hidden, FALSE otherwise.
	 */
	public boolean showPlayer(Player player) {
		String name = player.getPlayerListName();
		boolean success = hiddenPlayers.remove(name);

		sendInfoPacket(player, true);
		return success;
	}

	/**
	 * Determine if a given player is visible in the player list.
	 * 
	 * @param player - the player to check.
	 * @return TRUE if it is, FALSE otherwise.
	 */
	public boolean isVisible(Player player) {
		return !hiddenPlayers.contains(player.getName());
	}

	/**
	 * Retrieve the current ping value from a player.
	 * 
	 * @param player - player to retrieve.
	 * @return The ping value.
	 * @throws IllegalAccessException Unable to read the ping value due to a security limitation.
	 */
	public int getPlayerPing(Player player) throws IllegalAccessException {
		BukkitUnwrapper unwrapper = new BukkitUnwrapper();
		Object entity = unwrapper.unwrapItem(player);

		// Next, get the "ping" field
		if (pingField == null) {
			pingField = FuzzyReflection.fromObject(entity).getFieldByName("ping");
		}

		return (Integer) FieldUtils.readField(pingField, entity);
	}

	private void sendInfoPacket(Player player, boolean visible) {
		// BUILD
		WrapperPlayServerPlayerInfo packet = new WrapperPlayServerPlayerInfo();

		// Packet Action
		packet.setAction(visible ? PlayerInfoAction.ADD_PLAYER : PlayerInfoAction.REMOVE_PLAYER);

		// Packet Data
		int ping = 0;
		try {
			ping = getPlayerPing(player);
		} catch (IllegalAccessException e) {e.printStackTrace();}
		List<PlayerInfoData> list = packet.getData();
		list.add(new PlayerInfoData(WrappedGameProfile.fromPlayer(player), ping, NativeGameMode.fromBukkit(player.getGameMode()), WrappedChatComponent.fromText(player.getName())));
		packet.setData(list);

		// SEND
		for(Player p : Bukkit.getOnlinePlayers()) {
			packet.sendPacket(p);
		}
	}

	/**
	 * Retrieve every hidden player.
	 * 
	 * @return String Set of hidden players' names
	 */
	public Set<String> getHiddenPlayers() {
		return Collections.unmodifiableSet(hiddenPlayers);
	}

	/**
	 * Cleanup this player list hook.
	 */
	public void cleanupAll() {
		if (overrideListener != null) {
			manager.removePacketListener(overrideListener);
			overrideListener = null;
		}
	}
}