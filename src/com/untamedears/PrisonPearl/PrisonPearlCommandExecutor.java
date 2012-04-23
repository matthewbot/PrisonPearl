package com.untamedears.PrisonPearl;

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
		Player player = null;
		if (sender instanceof Player)
			player = (Player)sender;
		
		System.out.println("onCommand " + player);
		if (args.length < 1)
			return false;
		
		System.out.println("args[0] " + args[0]);
		
		if (args[0].equals("locate") && player != null) {
			System.out.println("locate");
			PrisonPearl pp = pearlstorage.getByImprisoned(player);
			if (pp != null) {
				String world = pp.getLocation().getWorld().getName();
				Vector vec = pp.getLocation().toVector();
				String vecstr = vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
				if (pp.getHolder() != null) {
					sender.sendMessage("Your prison pearl is held by " + pp.getHolderName() + " at " + world + " " + vecstr);
				} else {
					sender.sendMessage("Your prison pearl was dropped at " + world + " " + vecstr);
				}
			} else {
				sender.sendMessage("You have no prison pearl!");
			}
			
			return true;
		}
		
		return false;
	}

}
