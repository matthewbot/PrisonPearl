package com.untamedears.PrisonPearl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class PrisonPearlPlugin extends JavaPlugin implements Listener, CommandExecutor {
	private PearlTagList taglist;
	private PrisonPearlStorage pearlstorage;

	// set up configuration
	// load pearls from storage
	// make pearls stack by durability value
	public void onEnable() {
		getConfig().options().copyDefaults(true);
		saveConfig();
		
		taglist = new PearlTagList(this, getConfig().getInt("tag_ticks"));
		pearlstorage = new PrisonPearlStorage(this, getConfig().getInt("summon_damage_ticks"));

		File ppfile = getPrisonPearlsFile();
		try {
			pearlstorage.load(ppfile);
		} catch (FileNotFoundException e) {
			System.out.println("Prison pearls data file does not exist, creating.");

			try {
				pearlstorage.save(ppfile);
			} catch (IOException e2) {
				throw new RuntimeException("Failed to create " + ppfile.getAbsolutePath(), e2);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load prison pearls from " + ppfile.getAbsolutePath(), e);
		}

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

		getServer().getPluginManager().registerEvents(this, this);
		getCommand("pplocate").setExecutor(this);
		getCommand("pplocateany").setExecutor(this);
		getCommand("ppfree").setExecutor(this);
		getCommand("ppfreeany").setExecutor(this);
		getCommand("ppsummon").setExecutor(this);
		getCommand("ppreturn").setExecutor(this);
		getCommand("ppkill").setExecutor(this);
	}

	// save pearls
	public void onDisable() {
		try {
			File file = getPrisonPearlsFile();
			if (file.exists())
				file.renameTo(new File(file.getAbsolutePath() + ".bak"));
			pearlstorage.save(getPrisonPearlsFile());
		} catch (IOException e) {
			throw new RuntimeException("Failed to save prison pearls to " + getPrisonPearlsFile().getAbsolutePath(), e);
		}
	}

	private File getPrisonPearlsFile() {
		return new File(getDataFolder(), "prisonpearls.txt");
	}
	
	// Announce the person in a pearl when a player holds it
	@EventHandler(priority=EventPriority.MONITOR)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (item == null)
			return;

		if (item.getType() == Material.ENDER_PEARL && item.getDurability() != 0) {
			Player player = event.getPlayer();
			PrisonPearl pp = pearlstorage.getByID(item.getDurability());

			if (pp != null) {
				player.sendMessage("Prison Pearl - " + pp.getImprisonedName());
			} else {
				item.setDurability((short)0);
				player.getInventory().setItem(event.getNewSlot(), item);	
			}
		}
	}

	// Announce the person in a pearl when a player clicks it
	// Track the location of a pearl
	// Forbid pearls from being put in storage minecarts
	@EventHandler(priority=EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();
		if (clicked != null && clicked.getType() == Material.ENDER_PEARL && clicked.getDurability() != 0) {
			PrisonPearl pp = pearlstorage.getByID(clicked.getDurability());
			if (pp != null) {
				if (event.getWhoClicked() instanceof Player)
					((Player)event.getWhoClicked()).sendMessage("Prison Pearl - " + pp.getImprisonedName());
			} else {
				clicked.setDurability((short)0);
				event.setCurrentItem(clicked);
			}
		}

		PrisonPearl pp = pearlstorage.getByItemStack(event.getCursor());
		if (pp == null)
			return;

		InventoryView view = event.getView();
		int rawslot = event.getRawSlot();
		InventoryHolder holder;
		if (view.convertSlot(rawslot) == rawslot) { // this means in the top inventory
			holder = view.getTopInventory().getHolder();
		} else {
			holder = view.getBottomInventory().getHolder();
		}
		
		if (holder instanceof StorageMinecart) {
			event.setCancelled(true);
		} else {
			updatePearl(pp, holder);
		}
	}

	// see playerSpawn
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		Location newloc = playerSpawn(player, player.getLocation());
		if (newloc != null)
			player.teleport(newloc);
	}
	
	// see playerSpawn
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Location newloc = playerSpawn(event.getPlayer(), event.getRespawnLocation());
		if (newloc != null)
			event.setRespawnLocation(newloc);
	}
	
	// Give players the prison motd if they're imprisoned and not being summoned
	// Send players to the real world if they've been freed while offline
	private Location playerSpawn(Player player, Location spawnloc) {
		World respawn = Bukkit.getWorld(getConfig().getString("respawn_world"));
		World prison = Bukkit.getWorld(getConfig().getString("prison_world"));
		
		PrisonPearl pp = pearlstorage.getByImprisoned(player);
		Location newloc=null;
		
		if (pp != null) {
			if (!pearlstorage.isSummoning(pp)) {
				if (spawnloc.getWorld() != prison)
					newloc = prison.getSpawnLocation();
				
				for (String line : getConfig().getStringList("prison_motd"))
					player.sendMessage(line);
			}
		} else if (spawnloc.getWorld() == prison) {
			newloc = respawn.getSpawnLocation();
			player.sendMessage("You've been freed!");
		}		
		
		return newloc;
	}
	
	// Create tags when somebody punches somebody else with a pearl
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player))
			return;

		Player damager = (Player)event.getDamager();
		Player player = (Player)event.getEntity();

		ItemStack item = damager.getItemInHand();
		if (item.getType() == Material.ENDER_PEARL && item.getDurability() == 0)
			taglist.tag(player, damager);
	}

	// Free pearls when right clicked
	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		PrisonPearl pp = pearlstorage.getByItemStack(event.getItem());
		if (pp == null)
			return;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if (m == Material.CHEST || m == Material.WORKBENCH || m == Material.FURNACE || m == Material.DISPENSER || m == Material.BREWING_STAND)
				return;
		} else if (event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		Player player = event.getPlayer();
		player.getInventory().setItemInHand(null);
		event.setCancelled(true);
		
		freePearl(pp, player.getLocation());
		player.sendMessage("You've freed " + pp.getImprisonedName());
	}
	
	// Expire any tags the player had
	// Imprison the player if he was tagged and logout_imprison is on
	// Free any prisonpearls he had in his posession
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		
		taglist.taggerExpired(player);

		if (getConfig().getBoolean("logout_imprison")) {
			PearlTag tag = taglist.taggedKilled(player);
			if (tag != null)
				imprisonPlayer(tag);
		}

		Inventory inv = event.getPlayer().getInventory();
		for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
			int slot = entry.getKey();
			PrisonPearl pp = pearlstorage.getByItemStack(entry.getValue());
			if (pp == null)
				continue;

			freePearl(pp, player.getLocation());
			inv.setItem(slot, null);
		}
	}

	// Expire any tags the player had
	// Imprison the player if he was tagged
	// Return the player if he was summoned
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player)event.getEntity();
		taglist.taggerExpired(player);

		PearlTag tag = taglist.taggedKilled(player);
		if (tag != null)
			imprisonPlayer(tag);
		
		PrisonPearl pp = pearlstorage.getByImprisoned(player);
		if (pp != null)
			returnPlayer(pp);
	}

	// Track the location of a pearl if it spawns as an item for any reason
	@EventHandler(priority=EventPriority.MONITOR)
	public void onItemSpawn(ItemSpawnEvent event) {
		Item item = event.getEntity();
		PrisonPearl pp = pearlstorage.getByItemStack(item.getItemStack());
		if (pp == null)
			return;
		
		updatePearl(pp, item);
	}
	
	// Track the location of a pearl if a player picks it up
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		PrisonPearl pp = pearlstorage.getByItemStack(event.getItem().getItemStack());
		if (pp == null)
			return;
		
		updatePearl(pp, event.getPlayer());
	}
	
	// Free the pearl if it despawns
	@EventHandler(priority=EventPriority.MONITOR)
	public void onItemDespawn(ItemDespawnEvent event) {
		PrisonPearl pp = pearlstorage.getByItemStack(event.getEntity().getItemStack());
		if (pp == null)
			return;

		freePearl(pp, event.getEntity().getLocation());
	}

	// Free the pearl if its on a chunk that unloads
	@EventHandler(priority=EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		for (Entity e : event.getChunk().getEntities()) {
			if (!(e instanceof Item))
				continue;
			
			final PrisonPearl pp = pearlstorage.getByItemStack(((Item)e).getItemStack());
			if (pp == null)
				continue;	

			final Entity entity = e;
			Bukkit.getScheduler().callSyncMethod(this, new Callable<Void>() { // doing this in onChunkUnload causes weird things to happen
				public Void call() throws Exception {
					freePearl(pp, entity.getLocation());
					entity.remove();
					return null;
				}	
			});

			event.setCancelled(true);
		}
	}

	// Free the pearl if it combusts in lava/fire
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityCombustEvent(EntityCombustEvent event) {
		if (!(event.getEntity() instanceof Item))
			return;

		PrisonPearl pp = pearlstorage.getByItemStack(((Item)event.getEntity()).getItemStack());
		if (pp == null)
			return;

		freePearl(pp, event.getEntity().getLocation());
	}

	// Announce pearl tag events
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPearlTagEvent(PearlTagEvent event) {
		Player tagger = event.getTag().getTaggerPlayer();
		Player tagged = event.getTag().getTaggedPlayer();
		Player other = event.getOtherPlayer();

		if (event.getType() == PearlTagEvent.Type.NEW) {
			tagger.sendMessage("You've tagged " + tagged.getDisplayName() + " for imprisonment");
			tagged.sendMessage("You've been tagged for imprisonment by " + tagger.getDisplayName());
		} else if (event.getType() == PearlTagEvent.Type.EXPIRED) {
			tagger.sendMessage("Your tag expired for " + tagged.getDisplayName());
			tagged.sendMessage("You are no longer tagged by " + tagger.getDisplayName());
		} else if (event.getType() == PearlTagEvent.Type.SWITCHED) {
			tagger.sendMessage("Your tag for " + tagged.getDisplayName() + " is now owned by " + other.getDisplayName());
		} else if (event.getType() == PearlTagEvent.Type.KILLED){
			tagger.sendMessage("You've bound " + tagged.getDisplayName() + " to a prison pearl!");
			tagged.sendMessage("You've been bound to a prison pearl owned by " + tagger.getDisplayName());
		}
	}

	// Announce prison pearl events
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		World prisonworld = Bukkit.getWorld(getConfig().getString("prison_world"));
		
		PrisonPearl pp = event.getPrisonPearl();
		Player player = pp.getImprisonedPlayer();
		if (player == null || player.isDead()) // player not online or dead?
			return; // gets no intel then

		String world = pp.getLocation().getWorld().getName();
		Vector vec = pp.getLocation().toVector();
		String vecstr = vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
		
		switch (event.getType()) {
		case HELD:
			player.sendMessage("Your prison pearl is now held by " + pp.getHolderName() + " at " + world + " " + vecstr);
			break;

		case DROPPED:
			player.sendMessage("Your prison pearl was dropped at " + world + " " + vecstr);
			break;
			
		case SUMMONED:
			player.sendMessage("You've been summoned to your prison pearl!");
			player.teleport(pp.getLocation());
			break;
			
		case RETURNED:
			player.sendMessage("You've been returned to your prison");
			player.teleport(prisonworld.getSpawnLocation());
			break;
			
		case FREED:
			player.sendMessage("You've been freed!");
			if (event.getLocation() != null)
				player.teleport(event.getLocation());
			break;
		}
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
	
	private boolean freeCmd(CommandSender sender, String args[], boolean any) {
		PrisonPearl pp;
		Location loc = null;
		
		if (!any) {
			if (args.length > 1)
				return false;
			
			if (!(sender instanceof Player)) {
				sender.sendMessage("Must use freeany at console");
				return true;
			}
			
			Player player = (Player)sender;
			loc = player.getLocation();
			
			int slot = getCommandPearlSlot(player, args);
			if (slot == -1)
				return true;
			
			pp = pearlstorage.getByItemStack(player.getInventory().getItem(slot));
			player.getInventory().setItem(slot, null);		
		} else {
			if (args.length != 1)
				return false;
			
			pp = pearlstorage.getByImprisoned(args[0]);
			
			if (pp == null) {
				sender.sendMessage(args[0] + " is not imprisoned");
				return true;
			}
		}
		
		if (pp.getImprisonedPlayer() != sender)
			sender.sendMessage("You've freed " + pp.getImprisonedName());
		else
			loc = null;
		
		freePearl(pp, loc);	
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
		} else if (pearlstorage.isSummoning(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " is already summoned");
			return true;
		}
			
		sender.sendMessage("You've summoned " + pp.getImprisonedName());
		summonPlayer(pp);	
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
		
		if (pp.getImprisonedPlayer() == player) {
			sender.sendMessage("You cannot return yourself!");
			return true;
		} else if (!pearlstorage.isSummoning(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
			return true;
		}
			
		sender.sendMessage("You've returned " + pp.getImprisonedName());
		returnPlayer(pp);	
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
	
		if (!pearlstorage.isSummoning(pp)) {
			sender.sendMessage(pp.getImprisonedName() + " has not been summoned!");
			return true;
		}
			
		sender.sendMessage("You've killed " + pp.getImprisonedName());
		pp.getImprisonedPlayer().setHealth(0);
		return true;		
	}
	
	private PrisonPearl getCommandPearl(Player player, String args[]) {
		int slot = getCommandPearlSlot(player, args);
		if (slot != -1)
			return pearlstorage.getByItemStack(player.getInventory().getItem(slot));
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
			
			if (pearlstorage.getByItemStack(item) == null) {
				player.sendMessage("This is an ordinary ender pearl");
				return -1;
			}
			
			return player.getInventory().getHeldItemSlot();
		} else {		
			PrisonPearl pp = pearlstorage.getByImprisoned(args[0]);
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

	private boolean imprisonPlayer(PearlTag tag) {
		Player imprisoner = tag.getTaggerPlayer();
		Player imprisoned = tag.getTaggedPlayer();
		
		return imprisonPlayer(imprisoned, imprisoner);
	}
	
	private boolean imprisonPlayer(Player imprisoned, Player imprisoner) {
		World respawnworld = Bukkit.getWorld(getConfig().getString("respawn_world"));
		
		// set up the imprisoner's inventory
		Inventory inv = imprisoner.getInventory();
		ItemStack stack = null;
		int stacknum = -1;
		
		// scan for the smallest stack of normal ender pearls
		for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
			ItemStack newstack = entry.getValue();
			int newstacknum = entry.getKey();
			if (newstack.getDurability() == 0) {
				if (stack != null) {
					// don't keep a stack bigger than the previous one
					if (newstack.getAmount() > stack.getAmount())
						continue;
					// don't keep an identical sized stack in a higher slot
					if (newstack.getAmount() == stack.getAmount() && newstacknum > stacknum)
						continue;
				}
				
				stack = newstack;	
				stacknum = entry.getKey();
			}
		}
		
		if (stacknum == -1)
			return false; // imprisoner doesn't have normal pearl any more, so no go
		
		int pearlnum;
		if (stack.getAmount() == 1) { // if he's just got one pearl
			pearlnum = stacknum; // put the prison pearl there
		} else {
			pearlnum = inv.firstEmpty(); // otherwise, put the prison pearl in the first empty slot
			if (pearlnum > 0) {
				stack.setAmount(stack.getAmount()-1); // and reduce his stack of pearls by one
				inv.setItem(stacknum, stack);
			} else { // no empty slot?
				pearlnum = stacknum; // then overwrite his stack of pearls
			}
		}

		PrisonPearl pp = pearlstorage.imprison(imprisoned, imprisoner); // create the prison pearl
		inv.setItem(pearlnum, new ItemStack(Material.ENDER_PEARL, 1, pp.getID())); // give it to the imprisoner
		imprisoned.setBedSpawnLocation(respawnworld.getSpawnLocation()); // reset the player's normal spawn location
		
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, PrisonPearlEvent.Type.NEW)); // set off an event
		return true;
	}
	
	private void summonPlayer(PrisonPearl pp) {
		pearlstorage.setSummoning(pp, true);
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, PrisonPearlEvent.Type.SUMMONED));
	}
	
	private void returnPlayer(PrisonPearl pp) {
		pearlstorage.setSummoning(pp, false);
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, PrisonPearlEvent.Type.RETURNED));
	}
	
	private void updatePearl(PrisonPearl pp, Item item) {
		pp.setItem(item);
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, PrisonPearlEvent.Type.DROPPED));
	}
	
	private void updatePearl(PrisonPearl pp, InventoryHolder holder) {
		pp.setHolder(holder);
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, PrisonPearlEvent.Type.HELD));
	}
	
	private void freePearl(PrisonPearl pp, Location loc) {
		World respawnworld = Bukkit.getWorld(getConfig().getString("respawn_world"));
		World prisonworld = Bukkit.getWorld(getConfig().getString("prison_world"));
		
		if (loc != null && loc.getWorld() == prisonworld) // don't "free" players to the prison world
			loc = respawnworld.getSpawnLocation();
		pearlstorage.free(pp);
		
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, PrisonPearlEvent.Type.FREED, loc)); // set off an event
	}
}
