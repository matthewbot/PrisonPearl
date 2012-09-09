package com.untamedears.PrisonPearl;

import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

class PrisonPearlCommands implements CommandExecutor {
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	private final DamageLogManager damageman;
	private final PrisonPearlManager pearlman;
	private final SummonManager summonman;
	private final BroadcastManager broadcastman;
	
	public PrisonPearlCommands(PrisonPearlPlugin plugin, DamageLogManager damageman, PrisonPearlStorage pearls, PrisonPearlManager pearlman, SummonManager summonman, BroadcastManager broadcastman) {
		this.plugin = plugin;
		this.pearls = pearls;
		this.damageman = damageman;
		this.pearlman = pearlman;
		this.summonman = summonman;
		this.broadcastman = broadcastman;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("pplocate") || label.equalsIgnoreCase("ppl")) {
			return locateCmd(sender, args, false);
		} else if (label.equalsIgnoreCase("pplocateany")) {
			return locateCmd(sender, args, true);
		} else if (label.equalsIgnoreCase("ppfree") || label.equalsIgnoreCase("ppf")) {
			return freeCmd(sender, args, false);
		} else if (label.equalsIgnoreCase("ppfreeany")) {
			return freeCmd(sender, args, true);
		} else if (label.equalsIgnoreCase("ppsummon") || label.equalsIgnoreCase("pps")) {
			return summonCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppreturn") || label.equalsIgnoreCase("ppr")) {
			return returnCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppkill") || label.equalsIgnoreCase("ppk")) {
			return killCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppsave")) {
			return saveCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppimprisonany")) {
			return imprisonCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppbroadcast")) {
			return broadcastCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppconfirm")) {
			return confirmCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppsilence")) {
			return silenceCmd(sender, args);
		} else if (label.equalsIgnoreCase("pploadalts")) {
			return reloadAlts(sender);
		} else if (label.equalsIgnoreCase("ppcheckall")) {
			return checkAll(sender);
		} else if (label.equalsIgnoreCase("ppcheck")) {
			return check(sender, args);
		} else if (label.equalsIgnoreCase("kill")) {
			return kill();
		} else if (label.equalsIgnoreCase("ppsetdist")) {
            return setDistCmd(sender, args);
        } else if (label.equalsIgnoreCase("ppsetdamage")) {
            return setDamageCmd(sender, args);
        } else if (label.equalsIgnoreCase("pptogglespeech")) {
            return toggleSpeechCmd(sender, args);
        } else if (label.equalsIgnoreCase("pptoggledamage")) {
            return toggleDamageCmd(sender, args);
        } else if (label.equalsIgnoreCase("pptoggleblocks")) {
            return toggleBlocksCmd(sender, args);
        } else if (label.equalsIgnoreCase("ppsetmotd")) {
            return setMotdCmd(sender, args);
        }

		return false;
	}

    private PrisonPearl setCmd(CommandSender sender, String[] args) {
        PrisonPearl pp;

        if (!(sender instanceof Player)) {
            sender.sendMessage("ppset cannot be used at the console");
            return null;
        }

        String[] anArray = {};
        Player player = (Player)sender;
        pp = getCommandPearl(player, anArray, 1);

        if (pp == null){
            return null;
        }

        if (args.length > 1)
            return null;

        if (pp.getImprisonedPlayer().isDead()) {
            sender.sendMessage(pp.getImprisonedName() + " is dead. Bring him back to try again.");
            return null;
        } else if (pp.getImprisonedPlayer() == player) {
            sender.sendMessage("You cannot alter your own pearl!");
            return null;
        } else if (!(summonman.isSummoned(pp))) {
            sender.sendMessage(pp.getImprisonedName() + " is not summoned.");
            return null;
        }

        return pp;
    }

    @SuppressWarnings("SameReturnValue")
    private boolean setDistCmd(CommandSender sender, String args[]) {

        PrisonPearl pp = setCmd(sender, args);

        if (pp == null) {

            return false;
        }

        summonman.getSummon(pp.getImprisonedName()).setAllowedDistance(Integer.parseInt(args[0]));
        sender.sendMessage(pp.getImprisonedName() + "'s allowed distance set to " + args[0]);
        return true;
    }

    private boolean setDamageCmd(CommandSender sender, String args[]) {

        PrisonPearl pp = setCmd(sender, args);

        if (pp == null) {

            return false;
        }

        summonman.getSummon(pp.getImprisonedName()).setDamageAmount(Integer.parseInt(args[0]));
        sender.sendMessage(pp.getImprisonedName() + "'s damage amount set to " + args[0]);
        return true;
    }

    private boolean toggleSpeechCmd(CommandSender sender, String args[]) {

        PrisonPearl pp = setCmd(sender, args);

        if (pp == null) {

            return false;
        }

        boolean speak = summonman.getSummon(pp.getImprisonedName()).isCanSpeak();
        summonman.getSummon(pp.getImprisonedName()).setCanSpeak(!speak);
        sender.sendMessage(pp.getImprisonedName() + " ability to speak set to " + !speak);
        return true;
    }

    private boolean toggleDamageCmd(CommandSender sender, String args[]) {

        PrisonPearl pp = setCmd(sender, args);

        if (pp == null) {

            return false;
        }

        boolean damage = summonman.getSummon(pp.getImprisonedName()).isCanDealDamage();
        summonman.getSummon(pp.getImprisonedName()).setCanDealDamage(!damage);
        sender.sendMessage(pp.getImprisonedName() + " ability to deal damage set to " + !damage);
        return true;
    }

    private boolean toggleBlocksCmd(CommandSender sender, String args[]) {

        PrisonPearl pp = setCmd(sender, args);

        if (pp == null) {

            return false;
        }

        boolean block = summonman.getSummon(pp.getImprisonedName()).isCanBreakBlocks();
        summonman.getSummon(pp.getImprisonedName()).setCanBreakBlocks(!block);
        sender.sendMessage(pp.getImprisonedName() + " ability to break blocks set to " + !block);
        return true;
    }

    private boolean setMotdCmd(CommandSender sender, String args[]) {

        PrisonPearl pp;

        if (!(sender instanceof Player)) {
            sender.sendMessage("ppset cannot be used at the console");
            return true;
        }

        String[] anArray = {};
        Player player = (Player)sender;
        pp = getCommandPearl(player, anArray, 1);

        if (pp == null) {

            return false;
        }

        String s = "";
        for (String arg : args) {
            s = s.concat(arg + " ");
        }
        pp.setMotd(s);
        sender.sendMessage(pp.getImprisonedName() + "'s Message of the Day set to " + s);
        return true;
    }

    private boolean locateCmd(CommandSender sender, String args[], boolean any) {
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
			pp = pearls.getByImprisoned((Player)sender);
		} else {
			if (args.length != 1)
				return false;
			
			name_is = args[0] + " is";
			name_possesive = args[0] + "'s";
			pp = pearls.getByImprisoned(args[0]);
		}
		
		if (pp != null) {
			if (!pp.verifyLocation()) {
				System.err.println("PrisonPearl for " + pp.getImprisonedName() + " didn't validate, so is now set free");
				pearlman.freePearl(pp);
			} else {
				sender.sendMessage(ChatColor.GREEN + name_possesive + " prison pearl is " + pp.describeLocation());
				if (sender instanceof Player && !any)
					broadcastman.broadcast((Player)sender, ChatColor.GREEN + pp.getImprisonedName() + ": " + pp.describeLocation());
			}
		} else {
			sender.sendMessage(name_is + " not imprisoned");
		}
		
		return true;
	}
	
	private boolean freeCmd(CommandSender sender, String args[], boolean any) {
		PrisonPearl pp;
		
		if (!any) {
			if (args.length > 1)
				return false;
			
			if (!(sender instanceof Player)) {
				sender.sendMessage("Must use freeany at console");
				return true;
			}
			
			Player player = (Player)sender;
			
			int slot = getCommandPearlSlot(player, args, 0);
			if (slot == -1)
				return true;
			
			pp = pearls.getByItemStack(player.getInventory().getItem(slot));
			player.getInventory().setItem(slot, null);		
		} else {
			if (args.length != 1)
				return false;
			
			pp = pearls.getByImprisoned(args[0]);
			
			if (pp == null) {
				sender.sendMessage(args[0] + " is not imprisoned");
				return true;
			}
		}
		
		if (pearlman.freePearl(pp)) {
			if (pp.getImprisonedPlayer() != sender) // when freeing yourself, you're already going to get a message
				sender.sendMessage("You've freed " + pp.getImprisonedName());
		} else {
			sender.sendMessage("You failed to free " + pp.getImprisonedName());
		}
		return true;
	}
	
	private boolean imprisonCmd(CommandSender sender, String args[]) {
		if (args.length != 1)
			return false;
		if (!(sender instanceof Player)) {
			sender.sendMessage("imprison cannot be used at the console");
			return true;
		}
		
		if (pearlman.imprisonPlayer(args[0], (Player)sender)) {
			sender.sendMessage("You imprisoned " + args[0]);
			Player player = Bukkit.getPlayerExact(args[0]);
			if (player != null)
				player.setHealth(0);
		} else {
			sender.sendMessage("You failed to imprison " + args[0]);
		}
		return true;
	}
	
	private boolean summonCmd(CommandSender sender, String args[]) {
		if (args.length > 1)
			return false;
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("Command cannot be used at console");
			return true;
		}
		Player player = (Player)sender;

		PrisonPearl pp;

		if (args.length == 1) {
			try {
				pp = getCommandPearl(player, args, 0);
			} catch (NumberFormatException e) {
				pp = getCommandPearl(player, args, 1);
			}
		} else {
			pp = getCommandPearl(player, args, 0);
		}
		
		if (pp == null)
			return true;
		
		if (pp.getImprisonedPlayer() == null || pp.getImprisonedPlayer().isDead()) {
			sender.sendMessage(pp.getImprisonedName() + " cannot be summoned");
			return true;
		} else if (pp.getImprisonedPlayer() == player) {
			sender.sendMessage("You cannot summon yourself!");
			return true;
		} else if (summonman.isSummoned(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " is already summoned");
			return true;
		}
			
		if (summonman.summonPearl(pp))
			sender.sendMessage("You've summoned " + pp.getImprisonedName());
		else
			sender.sendMessage("You failed to summon " + pp.getImprisonedName());
		return true;		
	}
	
	private boolean returnCmd(CommandSender sender, String args[]) {
		if (args.length > 1)
			return false;
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("Command cannot be used at console");
			return true;
		}
		
		Player player = (Player)sender;
		PrisonPearl pp = getCommandPearl(player, args, 0); 
		if (pp == null)
			return true;
		
		if (pp.getImprisonedName().equals(player.getName())) {
			sender.sendMessage("You cannot return yourself!");
			return true;
		} else if (!summonman.isSummoned(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
			return true;
		} else if (damageman.hasDamageLog(player)) {
			sender.sendMessage(pp.getImprisonedName() + " is in combat and cannot be returned!");
			return true;
		}
			
		if (summonman.returnPearl(pp))
			sender.sendMessage("You've returned " + pp.getImprisonedName());
		else
			sender.sendMessage("You failed to return " + pp.getImprisonedName());
		return true;		
	}
	
	private boolean killCmd(CommandSender sender, String args[]) {
		if (args.length > 1)
			return false;
		
		if (!(sender instanceof Player)) {
			sender.sendMessage("Command cannot be used at console");
			return true;
		}
		
		Player player = (Player)sender;
		PrisonPearl pp = getCommandPearl(player, args, 0);
		if (pp == null)
			return true;
	
		if (!summonman.isSummoned(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
			return true;
		}
		
		if (summonman.killPearl(pp))
			sender.sendMessage("You've killed " + pp.getImprisonedName());
		else
			sender.sendMessage("You failed to kill " + pp.getImprisonedName());
		return true;		
	}
	
	private boolean saveCmd(CommandSender sender, String args[]) {
		if (args.length > 0)
			return false;
		
		try {
			plugin.saveAll(true);
			sender.sendMessage("PrisonPearl data saved!");
			return true;
		} catch (RuntimeException e) {
			if (!(sender instanceof ConsoleCommandSender))
				sender.sendMessage("PrisonPearl failed to save data! Check server logs!");
			throw e;
		}
	}
	
	private boolean broadcastCmd(CommandSender sender, String args[]) {
		if (args.length != 1)
			return false;
		if (!(sender instanceof Player)) {
			sender.sendMessage("Command cannot be used at console");
			return true;
		}
		
		Player player = (Player)sender;
		Player receiver = Bukkit.getPlayerExact(args[0]);
		if (receiver == null) {
			sender.sendMessage("No such player " + args[0]);
			return true;
		} else if (receiver == player) {
			sender.sendMessage("You cannot broadcast to yourself!");
			return true;
		} else if (!pearls.isImprisoned(player)) {
			sender.sendMessage("You are not imprisoned!");
			return true;
		}
		
		if (broadcastman.addBroadcast(player, receiver)) {
			sender.sendMessage("You will broadcast pplocate information to " + receiver.getDisplayName());
			receiver.sendMessage("Type /ppconfirm to receive pplocate broadcasts from " + player.getDisplayName());
		} else {
			sender.sendMessage("You are already broadcasting to " + receiver.getDisplayName());
		}
		
		return true;
	}
	
	private boolean confirmCmd(CommandSender sender, String args[]) {
		if (args.length > 1)
			return false;
		if (!(sender instanceof Player)) {
			sender.sendMessage("Command cannot be used at console");
			return true;
		}
		
		Player player = (Player)sender;
		Player broadcaster;
		
		if (args.length == 1) {
			broadcaster = Bukkit.getPlayerExact(args[0]);
			if (broadcaster == null) {
				sender.sendMessage("No such player " + args[0]);
				return true;
			}
		} else {
			broadcaster = broadcastman.getQuickConfirmPlayer(player);
			if (broadcaster == null) {
				sender.sendMessage("Nobody has requested to broadcast to you");
				return true;
			}
		}
		
		if (broadcastman.confirmBroadcast(broadcaster, player)) {
			player.sendMessage("You will now receive broadcasts from " + broadcaster.getDisplayName());
		} else {
			player.sendMessage(broadcaster.getDisplayName() + " does not wish to broadcast to you");
		}
		return true;
	}
	
	private boolean silenceCmd(CommandSender sender, String args[]) {
		if (args.length != 1)
			return false;
		if (!(sender instanceof Player)) {
			sender.sendMessage("Command cannot be used at console");
			return true;
		}
		
		Player player = (Player)sender;
		Player broadcaster = Bukkit.getPlayerExact(args[0]);
		if (broadcaster == null) {
			sender.sendMessage("No such player " + args[0]);
			return true;
		}
		
		if (broadcastman.silenceBroadcast(player, broadcaster)) {
			player.sendMessage("You will no longer receive broadcasts from " + broadcaster.getDisplayName());
		} else {
			player.sendMessage(broadcaster.getDisplayName() + " is not broadcasting to you");
		}
		return true;
	}
	
	private PrisonPearl getCommandPearl(Player player, String args[], int pos) {
		int slot = getCommandPearlSlot(player, args, pos);
		if (slot != -1)
			return pearls.getByItemStack(player.getInventory().getItem(slot));
		else
			return null;
	}
	
	private int getCommandPearlSlot(Player player, String args[], int pos) {
		if (args.length <= pos) {
			ItemStack item = player.getItemInHand();
			if (item.getType() != Material.ENDER_PEARL) {
				player.sendMessage("You must hold a pearl or supply the player's name to use this command");
				return -1;
			}
			
			if (pearls.getByItemStack(item) == null) {
				player.sendMessage("This is an ordinary ender pearl");
				return -1;
			}
			
			return player.getInventory().getHeldItemSlot();
		} else {		
			PrisonPearl pp = pearls.getByImprisoned(args[pos]);
			if (pp != null) {
				Inventory inv = player.getInventory();
				for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
					if (entry.getValue().getDurability() == pp.getID())
						return entry.getKey();
				}
			}
			
			player.sendMessage("You don't possess " + args[0] + "'s prison pearl");
			return -1;
		}
	}
	
	private boolean reloadAlts(CommandSender sender) {
		if (!(sender instanceof Player)) {
			plugin.loadAlts();
			plugin.checkBanAllAlts();
			return true;
		}
		return false;
	}
	
	private boolean checkAll(CommandSender sender) {
		if (!(sender instanceof Player)) {
			plugin.checkBanAllAlts();
			return true;
		}
		return false;
	}
	
	private boolean check(CommandSender sender, String[] args) {
		if (args.length != 1)
			return false;
		if (!(sender instanceof Player)) {
			boolean isBanned = plugin.isTempBanned(args[0]);
			if (isBanned) {
				sender.sendMessage(args[0]+" is temp banned for having "+plugin.getImprisonedCount(args[0])+" imprisoned accounts: "+plugin.getImprisonedAltsString(args[0]));
			} else {
				sender.sendMessage(args[0]+" is not temp banned");
			}
			return true;
		}
		return false;
	}
	
	@SuppressWarnings("SameReturnValue")
    private boolean kill() {
		return false;
	}
}
