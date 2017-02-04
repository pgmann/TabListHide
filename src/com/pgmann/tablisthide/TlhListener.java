package com.pgmann.tablisthide;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class TlhListener implements Listener{
	private TabListHide p;

	protected TlhListener(TabListHide p) {
		this.p=p;
	}
	
	@EventHandler
	protected void onPlayerJoin(PlayerJoinEvent e) {
		if(e.getPlayer().hasPermission("tablisthide.hide")) {
			p.setPlayerVisible(e.getPlayer(), false, true);
		}
	}
}
