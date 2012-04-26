package com.untamedears.PrisonPearl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

public class PrisonPearlPlugin extends JavaPlugin implements Listener, CommandExecutor {
	private DamageLogManager damageman;
	private PrisonPearlManager pearlman;
	private PrisonPearlStorage pearls;
	private SummonManager summonman;
	
	private Map<String, PermissionAttachment> attachments;

	public void onEnable() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		pearls = new PrisonPearlStorage();
		load(pearls, getPrisonPearlsFile());
		
		damageman = new DamageLogManager(this);
		pearlman = new PrisonPearlManager(this, pearls);
		summonman = new SummonManager(this, pearls);
		load(summonman, getSummonFile());
		
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				saveAll(false);
			}
		}, 0, getConfig().getLong("save_ticks"));
		
		getCommand("pplocate").setExecutor(this);
		getCommand("pplocateany").setExecutor(this);
		getCommand("ppfree").setExecutor(this);
		getCommand("ppfreeany").setExecutor(this);
		getCommand("ppsummon").setExecutor(this);
		getCommand("ppreturn").setExecutor(this);
		if (getConfig().getBoolean("ppkill_enabled"))
			getCommand("ppkill").setExecutor(this);
		getCommand("ppsave").setExecutor(this);

		// shamelessly swiped from bookworm, not sure why there isn't a Bukkit API for this
		// this causes items to be stacked by their durability value
		try {
			Method method = net.minecraft.server.Item.class.getDeclaredMethod("a", boolean.class);
			if (method.getReturnType() == net.minecraft.server.Item.class) {
				method.setAccessible(true);
				method.invoke(net.minecraft.server.Item.ENDER_PEARL, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		attachments = new HashMap<String, PermissionAttachment>();
		for (Player player : Bukkit.getOnlinePlayers())
			updateAttachment(player);
	}

	public void onDisable() {
		saveAll(true);
		
		for (PermissionAttachment attachment : attachments.values())
			attachment.remove();
	}
	
	private void saveAll(boolean force) {
		if (force || pearls.isDirty())
			save(pearls, getPrisonPearlsFile());
		if (force || summonman.isDirty())
			save(summonman, getSummonFile());
	}
	
	private static void load(SaveLoad obj, File file) {
		try {
			obj.load(file);
		} catch (FileNotFoundException e) {
			System.out.println(file.getName() + " not exist, creating.");

			try {
				obj.save(file);
			} catch (IOException e2) {
				throw new RuntimeException("Failed to create " + file.getAbsolutePath(), e2);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load prison pearls from " + file.getAbsolutePath(), e);
		}
	}
	
	private static void save(SaveLoad obj, File file) {
		try {
			File newfile = new File(file.getAbsolutePath() + ".new");
			File bakfile = new File(file.getAbsolutePath() + ".bak");
			
			obj.save(newfile);
			if (file.exists() && !file.renameTo(bakfile))
				throw new IOException("Failed to rename " + file.getAbsolutePath() + " to " + bakfile.getAbsolutePath());
			if (!newfile.renameTo(file))
				throw new IOException("Failed to rename " + newfile.getAbsolutePath() + " to " + file.getAbsolutePath());
		} catch (IOException e) {
			throw new RuntimeException("Failed to save prison pearls to " + file.getAbsolutePath(), e);
		}
	}

	private File getPrisonPearlsFile() {
		return new File(getDataFolder(), "prisonpearls.txt");
	}
	
	private File getSummonFile() {
		return new File(getDataFolder(), "summons.txt");
	}
	
	// go through player spawn logic in playerSpawn, teleport them as needed
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		updateAttachment(player);
		
		if (player.isDead())
			return;
		
		final Location newloc = playerSpawn(player, player.getLocation());
		if (newloc != null) {
			// under very specific situations it seems like teleport directly in onPlayerJoin causes duplicate entities
			Bukkit.getScheduler().callSyncMethod(this, new Callable<Void>() {
				public Void call() {
					World respawn = Bukkit.getWorld(getConfig().getString("respawn_world"));
					
					// If we're configured to respawn players who were free'd offline, and this player
					// needs to be moved to the real world (was freed)
					if (getConfig().getBoolean("free_offline_respawn") && newloc.getWorld() == respawn)
						player.setHealth(0); // kill them, to trigger the respawn
					else
						player.teleport(newloc); // otherwise, teleport them where they need t obe
					return null;
				}
			});
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		PermissionAttachment attachment = attachments.remove(event.getPlayer().getName());
		if (attachment != null)
			attachment.remove();
	}

	// run player spawn logic in playerSpawn
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Location newloc = playerSpawn(event.getPlayer(), event.getRespawnLocation());
		if (newloc != null)
			event.setRespawnLocation(newloc);
	}
	
	// tp people in and out of the prison world
	private Location playerSpawn(Player player, Location spawnloc) {
		World respawn = Bukkit.getWorld(getConfig().getString("respawn_world"));
		World prison = Bukkit.getWorld(getConfig().getString("prison_world"));
		Location newloc=null;
		
		if (pearls.isImprisoned(player)) { // if player is imprisoned
			if (!summonman.isSummoned(player)) { // and not summoned
				if (spawnloc.getWorld() != prison) // make sure he spawns in the prison world
					newloc = prison.getSpawnLocation();
				
				for (String line : getConfig().getStringList("prison_motd")) // give him prison_motd
					player.sendMessage(line);
			}
		} else if (spawnloc.getWorld() == prison) { // not imprisoned, but spawning in prison?
			newloc = player.getBedSpawnLocation(); // he must've been free'd while offline, change location to bed spawn
			if (newloc == null)
				newloc = respawn.getSpawnLocation(); // failing that, use the respawn world
			player.sendMessage("You were freed!");
		}		
		
		return newloc;
	}	
	
	// Imprison people upon death
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		
		Player player = (Player)event.getEntity();
		World prison = Bukkit.getWorld(getConfig().getString("prison_world"));
		
		if (player.getLocation().getWorld() == prison) // don't allow people to imprison other people while in the prison world
			return;
		
		PrisonPearl pp = pearls.getByImprisoned(player);
		if (getConfig().getBoolean("prisonerstealing_enabled") == false && pp != null) // bail if we can't steal prisoners and the guy is already imprisoned
			return;
		
		List<Player> damagers = damageman.getDamagers(player); // get all the players who helped kill this guy
		if (damagers == null)
			return;
		
		for (Player damager : damagers) { // check to see if anyone can imprison him
			if (pp != null && pp.getHolder() == damager) // if this damager has already imprisoned this person
				break; // don't be confusing and re-imprison him, just let him die
			if (pearlman.imprisonPlayer(player, damager)) // otherwise, try to imprison
				break;
		}
	}

	// Announce prison pearl events
	// Teleport players when freed
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		if (event.isCancelled())
			return;
		
		PrisonPearl pp = event.getPrisonPearl();
		Player player = pp.getImprisonedPlayer();
		if (player == null)
			return;
		
		if (event.getType() == PrisonPearlEvent.Type.NEW) {
			updateAttachment(player);
			
			Player imprisoner = event.getImprisoner();
			imprisoner.sendMessage("You've bound " + player.getDisplayName() + " to a prison pearl!");
			player.sendMessage("You've been bound to a prison pearl owned by " + imprisoner.getDisplayName());
		} else if (event.getType() == PrisonPearlEvent.Type.DROPPED || event.getType() == PrisonPearlEvent.Type.HELD) {
			player.sendMessage("Your prison pearl is " + pp.describeLocation());
		} else if (event.getType() == PrisonPearlEvent.Type.FREED) {
			updateAttachment(player);
			
			World respawnworld = Bukkit.getWorld(getConfig().getString("respawn_world"));
			World prisonworld = Bukkit.getWorld(getConfig().getString("prison_world"));
			
			if (player.getLocation().getWorld() == prisonworld) {
				Location loc = pp.getLocation();
				if (loc == null || loc.getWorld() == prisonworld) 
					loc = respawnworld.getSpawnLocation();
				
				if (!player.isDead())
					player.teleport(loc);
			}
			
			player.sendMessage("You've been freed!");
		}
	}
	
	// Announce summon events
	// Teleport player when summoned or returned
	@EventHandler(priority=EventPriority.MONITOR)
	public void onSummonEvent(SummonEvent event) {
		if (event.isCancelled())
			return;
		
		PrisonPearl pp = event.getPrisonPearl();
		Player player = pp.getImprisonedPlayer();
		if (player == null)
			return;

		switch (event.getType()) {
		case SUMMONED:
			player.sendMessage("You've been summoned to your prison pearl!");
			player.teleport(event.getLocation());
			break;
			
		case RETURNED:
			player.sendMessage("You've been returned to your prison");
			player.teleport(event.getLocation());
			break;
			
		case KILLED:
			player.sendMessage("You've been struck down by your pearl!");
			break;
		}
	}
	
	private void updateAttachment(Player player) {
		PermissionAttachment attachment = attachments.get(player.getName());
		if (attachment == null) {
			attachment = player.addAttachment(this);
			attachments.put(player.getName(), attachment);
		}
		
		if (pearls.isImprisoned(player)) {
			for (String grant : getConfig().getStringList("prison_grant_perms"))
				attachment.setPermission(grant, true);
			for (String deny : getConfig().getStringList("prison_deny_perms"))
				attachment.setPermission(deny, false);			
		} else {
			for (String grant : getConfig().getStringList("prison_grant_perms"))
				attachment.unsetPermission(grant);
			for (String deny : getConfig().getStringList("prison_deny_perms"))
				attachment.unsetPermission(deny);		
		}
		
		player.recalculatePermissions();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("pplocate")) {
			return locateCmd(sender, args, false);
		} else if (label.equalsIgnoreCase("pplocateany")) {
			return locateCmd(sender, args, true);
		} else if (label.equalsIgnoreCase("ppfree")) {
			return freeCmd(sender, args, false);
		} else if (label.equalsIgnoreCase("ppfreeany")) {
			return freeCmd(sender, args, true);
		} else if (label.equalsIgnoreCase("ppsummon")) {
			return summonCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppreturn")) {
			return returnCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppkill")) {
			return killCmd(sender, args);
		} else if (label.equalsIgnoreCase("ppsave")) {
			return saveCmd(sender, args);
		}

		return false;
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
			sender.sendMessage(name_possesive + " prison pearl is " + pp.describeLocation());
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
			
			int slot = getCommandPearlSlot(player, args);
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
		
		if (pp.getImprisonedPlayer() != sender)
			sender.sendMessage("You've freed " + pp.getImprisonedName());
		
		pearlman.freePearl(pp);	
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
		PrisonPearl pp = getCommandPearl(player, args);
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
			
		sender.sendMessage("You've summoned " + pp.getImprisonedName());
		summonman.summonPearl(pp, player.getLocation());
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
		PrisonPearl pp = getCommandPearl(player, args); 
		if (pp == null)
			return true;
		
		if (pp.getImprisonedName().equals(player.getName())) {
			sender.sendMessage("You cannot return yourself!");
			return true;
		} else if (!summonman.isSummoned(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
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
		PrisonPearl pp = getCommandPearl(player, args);
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
			saveAll(false);
			sender.sendMessage("PrisonPearl data saved!");
			return true;
		} catch (RuntimeException e) {
			if (!(sender instanceof ConsoleCommandSender))
				sender.sendMessage("PrisonPearl failed to save data! Check server logs!");
			throw e;
		}
	}
	
	private PrisonPearl getCommandPearl(Player player, String args[]) {
		int slot = getCommandPearlSlot(player, args);
		if (slot != -1)
			return pearls.getByItemStack(player.getInventory().getItem(slot));
		else
			return null;
	}
	
	private int getCommandPearlSlot(Player player, String args[]) {
		if (args.length == 0) {
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
			PrisonPearl pp = pearls.getByImprisoned(args[0]);
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
}
