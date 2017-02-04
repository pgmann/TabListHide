/*
 * TabListHide - Hide or show players in the player list only (TAB).
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@SuppressWarnings({ "unused" })
public class TlhCommand implements CommandExecutor {

	private TabListHide p = null;
	private ArrayList<String> commands = new ArrayList<String>();

	public TlhCommand(TabListHide p) {
		this.p = p;

		commands.add("show");
		commands.add("hide");
	}

	/**
	 * Handles a command.
	 * 
	 * @param sender - The sender
	 * @param command - The executed command
	 * @param label - The alias used for this command
	 * @param args - The arguments given to the command
	 * 
	 * @author Amaury Carrade
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("tablisthide") && !command.getName().equalsIgnoreCase("tlh")) {
			return false; // Should never happen
		}

		if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
			help(sender, args);
			return true;
		}

		String subcommandName = args[0].toLowerCase();

		// First: subcommand existence.
		if (!this.commands.contains(subcommandName)) {
			sender.sendMessage(p.colourise(TabListHide.prefix + ChatColor.DARK_RED + "Invalid command. Use "
					+ ChatColor.RED + "/tlh help" + ChatColor.DARK_RED + " for a list of commands."));
			return true;
		}

		// Second: is the sender allowed?
		if (!isAllowed(sender, args)) {
			unauthorized(sender, args);
			return true;
		}

		// Third: instantiation
		try {
			Class<? extends TlhCommand> cl = this.getClass();
			Class[] parametersTypes = new Class[] { CommandSender.class, Command.class, String.class, String[].class };

			Method doMethod = cl.getDeclaredMethod("do" + WordUtils.capitalize(subcommandName), parametersTypes);

			doMethod.invoke(this, new Object[] { sender, command, label, args });

			return true;

		} catch (NoSuchMethodException e) {
			// Unknown method => unknown subcommand.
			sender.sendMessage(p.colourise(TabListHide.prefix + ChatColor.DARK_RED + "Invalid command. Use "
					+ ChatColor.RED + "/tlh help" + ChatColor.DARK_RED + " for a list of commands."));
			return true;

		} catch (SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			sender.sendMessage(p.colourise(TabListHide.prefix + ChatColor.DARK_RED
					+ "An error occured, see console for details. This is probably a bug, please report it!"));
			e.printStackTrace();
			return true; // An error message has been printed, so command was
							// technically handled.
		}
	}

	/**
	 * Prints the plugin main help page.
	 * 
	 * @param sender - The help will be displayer for this sender.
	 */
	private void help(CommandSender sender, String[] args) {

		sender.sendMessage(p.colourise("            ~~ " + TabListHide.rawPrefix + " ~~            "));

		sender.sendMessage(p.colourise(ChatColor.YELLOW + "/tlh help" + ChatColor.WHITE + ": Displays this help page"));
		if (isAllowed(sender, "show".split(" ")))
			sender.sendMessage(p.colourise(ChatColor.YELLOW + "/tlh show [player] [silent]" + ChatColor.WHITE
					+ ": Shows the specified player"));
		if (isAllowed(sender, "hide".split(" ")))
			sender.sendMessage(p.colourise(ChatColor.YELLOW + "/tlh hide [player] [silent]" + ChatColor.WHITE
					+ ": Hides the specified player"));
	}

	/**
	 * This method checks if an user is allowed to send a command.
	 * 
	 * @param sender
	 * @param args
	 * 
	 * @return boolean The allowance status.
	 */
	protected boolean isAllowed(CommandSender sender, String[] args) {

		// The console is always allowed
		if (!(sender instanceof Player)) {
			return true;
		}

		else {

			if (sender.isOp()) {
				return true;
			}

			if (args.length == 0 || args[0].equalsIgnoreCase("help")) { // Help
				return true;
			}

			// Centralized way to manage permissions
			String permission = null;

			switch (args[0]) {

			case "show":
			case "hide":
				permission = "tablisthide.admin";
				break;
			default:
				permission = "tablisthide"; // Should never happen. But, just in
											// case...
				break;
			}

			return ((Player) sender).hasPermission(permission);
		}
	}

	/**
	 * This method sends a message to a player who tries to use a command
	 * without permission.
	 * 
	 * @param sender
	 * @param args
	 */
	protected void unauthorized(CommandSender sender, String[] args) {
		if (args.length == 0) {
			return; // will never happen, but just in case of a mistake...
		}

		String message = null;
		switch (args[0]) {
		case "show":
		case "hide":
			message = "You can't change whether players are visible in the tab list!";
			break;
		}

		sender.sendMessage(p.colourise(TabListHide.prefix + ChatColor.DARK_RED + message));
	}

	/**
	 * This command shows the player specified (or the target).<br>
	 * Usage: /tlh show [player] [silent]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doShow(CommandSender sender, Command command, String label, String[] args) {
		Player target = null;

		if (args.length > 1) {
			// Specified from command argument
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sender.sendMessage(TabListHide.prefix + ChatColor.DARK_RED + "No player called " + ChatColor.RED
						+ args[1] + ChatColor.DARK_RED + " could be found!");
				return;
			}
		} else if (sender instanceof Player) {
			// Use command sender
			target = (Player) sender;
		} else {
			// From console
			sender.sendMessage("Usage: /tlh show <player> [silent]");
			return;
		}
		boolean silent = args.length > 2 && args[2].equalsIgnoreCase("true");

		boolean success = p.setPlayerVisible(target, true, silent);
		
		if(sender != target) {
			if(success) sender.sendMessage(TabListHide.prefix + ChatColor.YELLOW + target.getDisplayName() + ChatColor.WHITE + " is now visible in the tab list");
			else sender.sendMessage(TabListHide.prefix + ChatColor.RED + target.getDisplayName() + ChatColor.DARK_RED + " is already visible!");
		}
	}

	/**
	 * This command hides the player specified (or the target).<br>
	 * Usage: /tlh hide [player] [silent]
	 * 
	 * @param sender
	 * @param command
	 * @param label
	 * @param args
	 */
	private void doHide(CommandSender sender, Command command, String label, String[] args) {
		Player target = null;

		if (args.length > 1) {
			// Specified from command argument
			target = Bukkit.getPlayer(args[1]);
			if (target == null) {
				sender.sendMessage(TabListHide.prefix + ChatColor.DARK_RED + "No player called " + ChatColor.RED + args[1] + ChatColor.DARK_RED + " could be found!");
				return;
			}
		} else if (sender instanceof Player) {
			// Use command sender
			target = (Player) sender;
		} else {
			// From console
			sender.sendMessage("Usage: /tlh hide <player> [silent]");
			return;
		}
		boolean silent = args.length > 2 && args[2].equalsIgnoreCase("true");

		boolean success = p.setPlayerVisible(target, false, silent);
		
		if(sender != target) {
			if(success) sender.sendMessage(TabListHide.prefix + ChatColor.YELLOW + target.getDisplayName() + ChatColor.WHITE + " is now hidden from the tab list");
			else sender.sendMessage(TabListHide.prefix + ChatColor.RED + target.getDisplayName() + ChatColor.DARK_RED + " is already hidden!");
		}
	}
}
