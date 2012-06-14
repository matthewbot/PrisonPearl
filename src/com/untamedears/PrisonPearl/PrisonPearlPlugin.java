package com.untamedears.PrisonPearl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

public class PrisonPearlPlugin extends JavaPlugin implements Listener {
	private PrisonPearlStorage pearls;
	private DamageLogManager damageman;
	private PrisonPearlManager pearlman;
	private SummonManager summonman;
	private PrisonPortaledPlayerManager portalman;
	private BroadcastManager broadcastman;
	private AltsList altsList;
	public static Logger log = Bukkit.getLogger();
	private static Integer maxImprisonedAlts = 2;
	private static String kickMessage = "You have too many imprisoned alts accounts!";
	
	
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
		portalman = new PrisonPortaledPlayerManager(this, pearls);
		load(portalman, getPortaledPlayersFile());
		broadcastman = new BroadcastManager();

		loadAlts();
		
		if (Bukkit.getPluginManager().isPluginEnabled("PhysicalShop"))
			new PhysicalShopListener(this, pearls);
		
		Bukkit.getPluginManager().registerEvents(this, this);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				saveAll(false);
			}
		}, 0, getConfig().getLong("save_ticks"));
		
		PrisonPearlCommands commands = new PrisonPearlCommands(this, damageman, pearls, pearlman, summonman, broadcastman);
		for (String command : getDescription().getCommands().keySet()) {
			if (command.equals("ppkill") && !getConfig().getBoolean("ppkill_enabled"))
				continue;
			
			getCommand(command).setExecutor(commands);
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
		
		attachments = new HashMap<String, PermissionAttachment>();
		for (Player player : Bukkit.getOnlinePlayers())
			updateAttachment(player);
	}

	public void onDisable() {
		saveAll(true);
		
		for (PermissionAttachment attachment : attachments.values())
			attachment.remove();
	}
	
	public void saveAll(boolean force) {
		if (force || pearls.isDirty())
			save(pearls, getPrisonPearlsFile());
		if (force || summonman.isDirty())
			save(summonman, getSummonFile());
		if (force || portalman.isDirty())
			save(portalman, getPortaledPlayersFile());
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
	
	private File getPortaledPlayersFile() {
		return new File(getDataFolder(), "portaledplayers.txt");
	}
	
	
	private File getAltsListFile() {
		return new File(getDataFolder(), "alts.txt");
	}
	
	
	// Free player if he was free'd while offline
	// otherwise, correct his spawn location if necessary
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		updateAttachment(player);
		
		
		String[] altsArray = altsList.getAltsArray(player.getName());
		
		if (altsArray.length >= maxImprisonedAlts) {
			Integer pearledCount = pearls.getImprisonedCount(altsArray);
			String[] imprisonedNames = pearls.getImprisonedNames(altsArray);
			String names = "";
			for (int i = 0; i < imprisonedNames.length; i++) {
				names = names + imprisonedNames[i];
				if (i < imprisonedNames.length-1) {
					names = names + ", ";
				}
			}
			log.info("[PrisonPearl] "+player.getName()+" has "+ pearledCount +" imprisoned alts: "+names);
			if (pearledCount >= maxImprisonedAlts && !pearls.isImprisoned(player.getName())) {
				player.kickPlayer(kickMessage);
				log.info("[PrisonPearl] "+player.getName()+" disconnected for having too many alts imprisoned.");
			}
		}
		
		
		if (player.isDead())
			return;
		
		Location loc = player.getLocation();
		Location newloc = getRespawnLocation(player, loc);
		if (newloc != null) {
			if (loc.getWorld() == getPrisonWorld() && (newloc.getWorld() != loc.getWorld() || newloc == RESPAWN_PLAYER))
				player.sendMessage("While away, you were freed!"); // he was freed offline
			delayedTp(player, newloc);
		} else {
			prisonMotd(player); 
		}
	}
	
	// don't let people escape through the end portal
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerPortalEvent(PlayerPortalEvent event) {
		Player player = event.getPlayer();
		
		if (pearls.isImprisoned(player) && !summonman.isSummoned(player)) { // if in prison but not imprisoned
			if (event.getTo().getWorld() != getPrisonWorld()) {
				prisonMotd(player);
				delayedTp(player, getPrisonSpawnLocation());
			}
		}
	}
	
	// remove permission attachments
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		PermissionAttachment attachment = attachments.remove(event.getPlayer().getName());
		if (attachment != null)
			attachment.remove();
	}

	// adjust spawnpoint if necessary
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		prisonMotd(event.getPlayer());
		Location newloc = getRespawnLocation(event.getPlayer(), event.getRespawnLocation());
		if (newloc != null && newloc != RESPAWN_PLAYER)
			event.setRespawnLocation(newloc);
	}
	
	// called when a player joins or spawns
	private void prisonMotd(Player player) {
		if (pearls.isImprisoned(player) && !summonman.isSummoned(player)) { // if player is imprisoned
			for (String line : getConfig().getStringList("prison_motd")) // give him prison_motd
				player.sendMessage(line);
		}
	}	
	
	private static Location RESPAWN_PLAYER = new Location(null, 0, 0, 0);
	
	// gets where the player should be respawned at
	// returns null if the curloc is an acceptable respawn location
	private Location getRespawnLocation(Player player, Location curloc) {	
		if (pearls.isImprisoned(player)) { // if player is imprisoned
			if (summonman.isSummoned(player)) // if summoned
				return null; // don't modify location
			if (curloc.getWorld() != getPrisonWorld()) // but not in prison world
				return getPrisonSpawnLocation(); // should bre respawned in prison
		} else if (curloc.getWorld() == getPrisonWorld() && !portalman.isPlayerPortaledToPrison(player)) { // not imprisoned, but spawning in prison?
			if (player.getBedSpawnLocation() != null) // if he's got a bed
				return player.getBedSpawnLocation(); // spawn him there
			else if (getConfig().getBoolean("free_respawn")) // if we should respawn instead of tp to spawn
				return RESPAWN_PLAYER; // kill the player
			else
				return getFreeWorld().getSpawnLocation(); // otherwise, respawn him at the spawn of the free world
		}
		
		return null; // don't modify respawn location
	}
	
	// Imprison people upon death
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		
		Player player = (Player)event.getEntity();
		
		PrisonPearl pp = pearls.getByImprisoned(player); // find out if the player is imprisoned
		if (pp != null) { // if imprisoned
			if (!getConfig().getBoolean("prison_stealing") || player.getLocation().getWorld() == getPrisonWorld()) // bail if prisoner stealing isn't allowed, or if the player is in prison (can't steal prisoners from prison ever)
				return;
		}
		
		for (Player damager : damageman.getDamagers(player)) { // check to see if anyone can imprison him
			if (pp != null && pp.getHolderPlayer() == damager) // if this damager has already imprisoned this person
				break; // don't be confusing and re-imprison him, just let him die
			
			int firstpearl = Integer.MAX_VALUE; // find the first regular enderpearl in their inventory
			for (Entry<Integer, ? extends ItemStack> entry : damager.getInventory().all(Material.ENDER_PEARL).entrySet()) {
				if (entry.getValue().getDurability() == 0)
					firstpearl = Math.min(entry.getKey(), firstpearl);
			}
			
			if (firstpearl == Integer.MAX_VALUE) // no pearl
				continue; // no imprisonment
			
			if (getConfig().getBoolean("prison_musthotbar") && firstpearl > 9) // bail if it must be in the hotbar
				continue; 
				
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
			imprisoner.sendMessage(ChatColor.GREEN+"You've bound " + player.getDisplayName() + ChatColor.GREEN+" to a prison pearl!");
			player.sendMessage(ChatColor.RED+"You've been bound to a prison pearl owned by " + imprisoner.getDisplayName());

			Server s = this.getServer();
			String[] alts = altsList.getAltsArray(player.getName());
			for (int j = 0; j < alts.length; j++) {
				String[] tempAlts = altsList.getAltsArray(alts[j]);
				Integer count = pearls.getImprisonedCount(tempAlts);
				if (count >= maxImprisonedAlts) {
					for (int i = 0; i < tempAlts.length; i++) {
						if (!pearls.isImprisoned(tempAlts[i])) {
							Player p = s.getPlayer(tempAlts[i]);
							if (p != null) {
								p.kickPlayer(kickMessage);
							}
						}
					}
				}
			}
			
		} else if (event.getType() == PrisonPearlEvent.Type.DROPPED || event.getType() == PrisonPearlEvent.Type.HELD) {
			String loc = pp.describeLocation();
			player.sendMessage(ChatColor.GREEN + "Your prison pearl is " + loc);
			broadcastman.broadcast(player, ChatColor.GREEN + player.getName() + ": " + loc);
		} else if (event.getType() == PrisonPearlEvent.Type.FREED) {
			updateAttachment(player);
			
			if (!player.isDead() && player.getLocation().getWorld() == getPrisonWorld()) { // if the player isn't dead and is in prison world
				Location loc = null;
				if (getConfig().getBoolean("free_tppearl")) // if we tp to pearl on players being freed
					loc = fuzzLocation(pp.getLocation()); // get the location of the pearl
				if (loc == null) // if we don't have a location yet
					loc = getRespawnLocation(player, player.getLocation()); // get the respawn location for the player
				
				if (loc == RESPAWN_PLAYER) { // if we're supposed to respawn the player
					player.setHealth(0); // kill him
				} else {
					player.teleport(loc); // otherwise teleport
				}
			}
			
			player.sendMessage("You've been freed!");
			broadcastman.broadcast(player, player.getDisplayName() + " was freed!");
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
			player.sendMessage(ChatColor.RED+"You've been summoned to your prison pearl!");
			player.teleport(fuzzLocation(event.getLocation()));
			break;
			
		case RETURNED:
			player.sendMessage(ChatColor.RED+"You've been returned to your prison");
			player.teleport(event.getLocation());
			break;
			
		case KILLED:
			player.sendMessage(ChatColor.RED+"You've been struck down by your pearl!");
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
	
	
	private World getFreeWorld() {
		return Bukkit.getWorld(getConfig().getString("free_world"));
	}
	
	private World getPrisonWorld() {
		return Bukkit.getWorld(getConfig().getString("prison_world"));
	}
	
	// hill climbing algorithm which attempts to randomly spawn prisoners while actively avoiding pits
	// the obsidian pillars, or lava.
	private Location getPrisonSpawnLocation() {
		Random rand = new Random();
		Location loc = getPrisonWorld().getSpawnLocation(); // start at spawn
		for (int i=0; i<30; i++) { // for up to 30 iterations
			if (loc.getY() > 40 && loc.getY() < 70 && i > 5 && !isObstructed(loc)) // if the current candidate looks reasonable and we've iterated at least 5 times
				return loc; // we're done
			
			Location newloc = loc.clone().add(rand.nextGaussian()*(2*i), 0, rand.nextGaussian()*(2*i)); // pick a new location near the current one
			newloc = moveToGround(newloc);
			if (newloc == null)
				continue;
			
			if (newloc.getY() > loc.getY()+(int)(rand.nextGaussian()*3) || loc.getY() > 70) // if its better in a fuzzy sense, or if the current location is too high
				loc = newloc; // it becomes the new current location
		}

		return loc;
	}
	
	private Location moveToGround(Location loc) {
		Location ground = new Location(loc.getWorld(), loc.getX(), 100, loc.getZ());
		while (ground.getBlockY() >= 1) {
			if (!ground.getBlock().isEmpty())
				return ground;
			ground.add(0, -1, 0);
		}
		return null;
	}
	
	private boolean isObstructed(Location loc) {
		Location ground = new Location(loc.getWorld(), loc.getX(), 100, loc.getZ());
		while (ground.getBlockY() >= 1) {
			if (!ground.getBlock().isEmpty())
				break;
				
			ground.add(0, -1, 0);
		}
		
		for (int x=-2; x<=2; x++) {
			for (int y=-2; y<=2; y++) {
				for (int z=-2; z<=2; z++) {
					Location l = ground.clone().add(x, y, z);
					Material type = l.getBlock().getType();
					if (type == Material.LAVA || type == Material.STATIONARY_LAVA || type == Material.ENDER_PORTAL || type == Material.BEDROCK)
						return true;
				}
			}
		}
		
		return false;
	}
	
	private Location fuzzLocation(Location loc) {
		if (loc == null)
			return null;

		double rad = Math.random()*Math.PI*2;
		Location newloc = loc.clone();
		newloc.add(1.2*Math.cos(rad), 1.2*Math.sin(rad), 0);
		return newloc;
	}
	
	private void delayedTp(final Player player, final Location loc) {
		if (loc == RESPAWN_PLAYER) {
			player.setHealth(0);
		} else {
			Bukkit.getScheduler().callSyncMethod(this, new Callable<Void>() {
				public Void call() {
					player.teleport(loc);
					return null;
				}
			});
		}
	}
	
	public void loadAlts() {
		if (altsList == null) {
			altsList = new AltsList();
		}
		altsList.load(getAltsListFile());
	}
}
