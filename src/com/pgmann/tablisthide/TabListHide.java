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

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TabListHide extends JavaPlugin {
	TlhCommand commands;
	static String rawPrefix = ChatColor.YELLOW + "TabListHide" + ChatColor.WHITE;
	static String prefix = ChatColor.WHITE + "[" + rawPrefix + ChatColor.WHITE + "] ";
	HidePlayerList hpl;

	@Override
	public void onEnable() {
		getServer().getConsoleSender().sendMessage(rawPrefix + " by " + ChatColor.YELLOW + "pgmann" + ChatColor.WHITE + " is enabled!");

		// Register the command listener
		commands = new TlhCommand(this);
		getCommand("tablisthide").setExecutor(commands);
		getCommand("tlh").setExecutor(commands);

		// Register the event listener
		getServer().getPluginManager().registerEvents(new TlhListener(this), this);
		
		// Register ProtocolLib packet listener
		hpl = new HidePlayerList(this);
		hpl.register();
	}

	@Override
	public void onDisable() {
		getServer().getConsoleSender().sendMessage(rawPrefix + " is now disabled.");
	}

	protected String colourise(String rawText) {
		return ChatColor.translateAlternateColorCodes('&', rawText);
	}
	
	public boolean setPlayerVisible(Player player, boolean visible, boolean silent) {
		boolean success;
		
		if(visible) success = hpl.showPlayer(player);
		else success = hpl.hidePlayer(player);
		
		if (!silent && success)
			player.sendMessage(prefix + "You are now " + ChatColor.YELLOW + (visible ? "" : "in") + "visible"+ChatColor.WHITE+" on the tab list");
		
		return success;
	}
	public boolean isPlayerVisible(Player player) {
		return hpl.isVisible(player);
	}
}
