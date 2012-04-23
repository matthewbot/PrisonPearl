package com.untamedears.PrisonPearl;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PrisonPearlCommandExecutor implements CommandExecutor {
	private PrisonPearlStorage pearlstorage;
	
	public PrisonPearlCommandExecutor(PrisonPearlStorage pearlstorage) {
		this.pearlstorage = pearlstorage;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("pplocate")) {
			return locate(sender, args, false);
		} else if (label.equalsIgnoreCase("pplocateany")) {
			return locate(sender, args, true);
		} else if (label.equalsIgnoreCase("ppfree")) {
			return free(sender, args, false);
		} else if (label.equalsIgnoreCase("ppfreeany")) {
			return free(sender, args, true);
		}

		return false;
	}
	
	private boolean locate(CommandSender sender, String args[], boolean any) {
		String name_is;
		String name_possesive;
		PrisonPearl pp;
		
		if (!any) {
			if (args.length != 0)
				return false;
			
			if (!(sender instanceof Player)) {
				sender.sendMessage("Must use pplocateany at the console");
				return true;
			}
				
			name_is = "You are";
			name_possesive = "Your";
			pp = pearlstorage.getByImprisoned((Player)sender);
		} else {
			if (args.length != 1)
				return false;
			
			name_is = args[0] + " is";
			name_possesive = args[0] + "'s";
			pp = pearlstorage.getByImprisoned(args[0]);
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
			sender.sendMessage(name_is + " not imprisoned");
		}
		
		return true;
	}
	
	private boolean free(CommandSender sender, String args[], boolean any) {
		PrisonPearl pp;
		Location loc;
		
		if (!any) {
			if (args.length > 1)
				return false;
			
			if (!(sender instanceof Player)) {
				sender.sendMessage("Must use freeany at console");
				return true;
			}
			
			Player player = (Player)sender;
			loc = player.getLocation();
			
			if (args.length == 0) {
				ItemStack item = player.getItemInHand();
				if (item.getType() != Material.ENDER_PEARL) {
					sender.sendMessage("You must hold a pearl or supply the player's name to free a player");
					return true;
				}
				
				pp = pearlstorage.getByItemStack(item);
				if (pp == null) {
					sender.sendMessage("This is an ordinary ender pearl");
					return true;
				}
				
				player.setItemInHand(null);
			} else {				
				boolean found = false;
				
				pp = pearlstorage.getByImprisoned(args[0]);
				if (pp != null) {
					Inventory inv = player.getInventory();
					for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
						if (entry.getValue().getDurability() == pp.getID()) {
							inv.setItem(entry.getKey(), null);
							found = true;
							break;
						}
					}
				}
				
				if (!found) {
					sender.sendMessage("You don't possess " + args[0] + "'s prison pearl");
					return true;
				}
			}
		} else {
			if (args.length != 1)
				return false;
			
			loc = Bukkit.getWorlds().get(0).getSpawnLocation();
			pp = pearlstorage.getByImprisoned(args[0]);
			
			if (pp == null) {
				sender.sendMessage(args[0] + " is not imprisoned");
				return true;
			}
		}
		
		pearlstorage.free(pp, loc);
		sender.sendMessage("You've freed " + pp.getImprisonedName());
		return true;
	}
}
