package com.pgmann.tablisthide;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class TabListHide extends JavaPlugin {
	TlhCommand commands;
	static String rawPrefix = ChatColor.YELLOW + "TabListHide" + ChatColor.WHITE;
	static String prefix = ChatColor.WHITE + "[" + rawPrefix + ChatColor.WHITE + "] ";
	HidePlayerList hlp;

	@Override
	public void onEnable() {
		//initAllPlayerLists();
		
		getServer().getConsoleSender().sendMessage(rawPrefix + " by " + ChatColor.YELLOW + "pgmann" + ChatColor.WHITE + " is enabled!");

		// Register the command listener
		commands = new TlhCommand(this);
		getCommand("tablisthide").setExecutor(commands);
		getCommand("tlh").setExecutor(commands);

		// Register the event listener
		getServer().getPluginManager().registerEvents(new TlhListener(this), this);
		
		// Register ProtocolLib packet listener
		hlp = new HidePlayerList(this);
		hlp.register();
	}

	@Override
	public void onDisable() {
		getServer().getConsoleSender().sendMessage(rawPrefix + " is now disabled.");
	}

	public String colourise(String rawText) {
		return ChatColor.translateAlternateColorCodes('&', rawText);
	}
	
	public boolean setPlayerVisible(Player player, boolean visible, boolean silent) {
		boolean success;
		
		if(visible) success = hlp.showPlayer(player);
		else success = hlp.hidePlayer(player);
		
		if (!silent && success)
			player.sendMessage(prefix + "You are now " + ChatColor.YELLOW + (visible ? "" : "in") + "visible"+ChatColor.WHITE+" on the tab list");
		
		return success;
	}
}
