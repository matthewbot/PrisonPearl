package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class PrisonPearlCommandExecutor implements CommandExecutor {
	private PrisonPearlStorage pearlstorage;
	
	public PrisonPearlCommandExecutor(PrisonPearlStorage pearlstorage) {
		this.pearlstorage = pearlstorage;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length >= 1) {	
			if (args[0].equalsIgnoreCase("locate")) {
				return locate(cmd, sender, args);
			} else if (args[0].equalsIgnoreCase("free")) {
				return free(cmd, sender, args);
			} 
		}
		
		sender.sendMessage("Invalid command. Valid commands are locate, free");
		return true;
	}
	
	private boolean locate(Command cmd, CommandSender sender, String args[]) {
		String name_posses;
		String name_possesive;
		PrisonPearl pp;
		
		if (args.length == 1) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Must use /pp locate [playername] at the console");
				return true;
			}
				
			name_posses = "You have";
			name_possesive = "Your";
			pp = pearlstorage.getByImprisoned((Player)sender);
		} else if (args.length == 2) {
			if (!sender.hasPermission("prisonpearl.locate")) {
				sender.sendMessage(cmd.getPermissionMessage());
				return true;
			}
			
			name_posses = args[1] + " has";
			name_possesive = args[1] + "'s";
			pp = pearlstorage.getByImprisoned(args[1]);
		} else {
			sender.sendMessage("locate expects 0 or 1 arguments");
			return true;
		}
		
		if (pp != null) {
			String world = pp.getLocation().getWorld().getName();
			Vector vec = pp.getLocation().toVector();
			String vecstr = vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
			if (pp.getHolder() != null) {
				sender.sendMessage(name_possesive + " prison pearl is held by " + pp.getHolderName() + " at " + world + " " + vecstr);
			} else {
				sender.sendMessage(name_possesive + " prison pearl was dropped at " + world + " " + vecstr);
			}
		} else {
			sender.sendMessage(name_posses + " no prison pearl");
		}
		
		return true;
	}
	
	private boolean free(Command cmd, CommandSender sender, String args[]) {
		if (args.length != 2)
			return true;
		
		if (sender.hasPermission("prisonpearl.free")) {
			sender.sendMessage(cmd.getPermissionMessage());
			return true;
		}
		
		PrisonPearl pp = pearlstorage.getByImprisoned(args[1]);
		if (pp == null) {
			sender.sendMessage(args[1] + " has no prison pearl");
			return true;
		}
		
		pearlstorage.free(pp, Bukkit.getWorlds().get(0).getSpawnLocation());
		sender.sendMessage(args[1] + " is now free");
		return true;
	}

}
