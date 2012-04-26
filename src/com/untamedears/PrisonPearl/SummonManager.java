package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class SummonManager implements Runnable, Listener, SaveLoad {
	private PrisonPearlPlugin plugin;
	private PrisonPearlStorage pearls;
	
	private Map<String, Location> summoned_pearls;
	
	public SummonManager(PrisonPearlPlugin plugin, PrisonPearlStorage pearls) {
		this.plugin = plugin;
		this.pearls = pearls;
		
		summoned_pearls = new HashMap<String, Location>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, plugin.getConfig().getInt("summon_damage_ticks"));
	}
	
	public void load(File file) throws NumberFormatException, IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String line;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split(" ");
			String name = parts[0];
			Location loc = new Location(Bukkit.getWorld(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
			
			if (!pearls.isImprisoned(name))
				continue;
			
			summoned_pearls.put(name, loc);
		}
		
		fis.close();
	}
	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
		
		for (Entry<String, Location> entry : summoned_pearls.entrySet()) {
			Location loc = entry.getValue();
			br.append(entry.getKey() + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "\n");
		}
		
		br.flush();
		fos.close();
	}
	
	public void run() {
		Iterator<Entry<String, Location>> i = summoned_pearls.entrySet().iterator();
		while (i.hasNext()) {
			Entry<String, Location> entry = i.next();
			PrisonPearl pp = pearls.getByImprisoned(entry.getKey());
			if (pp == null) {
				System.err.println("Somehow " + entry.getKey() + " got summoned but isn't imprisoned");
				i.remove();
				continue;
			}
			
			Player player = pp.getImprisonedPlayer();
			if (player == null)
				continue;
			
			Location pploc = pp.getLocation();
			Location playerloc = pp.getImprisonedPlayer().getLocation();
			
			if (pploc.getWorld() != playerloc.getWorld() || pploc.distance(playerloc) > plugin.getConfig().getDouble("summon_damage_radius"))
				player.damage(plugin.getConfig().getInt("summon_damage_amt"));
		}
	}
	
	public boolean summonPearl(PrisonPearl pp, Location loc) {
		Player player = pp.getImprisonedPlayer();
		if (player == null || player.isDead())
			return false;
		
		if (!summonEvent(pp, SummonEvent.Type.SUMMONED, loc))
			return false;
		
		summoned_pearls.put(player.getName(), player.getLocation());
		return true;
	}
	
	public boolean returnPearl(PrisonPearl pp) {
		Location loc = summoned_pearls.remove(pp.getImprisonedName());
		if (loc == null || pp.getImprisonedPlayer().isDead())
			return false;
		
		if (!summonEvent(pp, SummonEvent.Type.RETURNED, loc))
			return false;

		return true;
	}
	
	public boolean killPearl(PrisonPearl pp) {
		if (summoned_pearls.remove(pp.getImprisonedName()) == null)
			return false;
		
		if (!summonEvent(pp, SummonEvent.Type.KILLED))
			return false;
		
		pp.getImprisonedPlayer().setHealth(0);
		return true;
	}
	
	public boolean isSummoned(Player player) {
		return summoned_pearls.containsKey(player.getName());
	}
	
	public boolean isSummoned(PrisonPearl pp) {
		return summoned_pearls.containsKey(pp.getImprisonedName());
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player)event.getEntity();
		if (!isSummoned(player))
			return;
		summoned_pearls.remove(player.getName());
		
		PrisonPearl pp = pearls.getByImprisoned(player);
		if (pp != null)
			summonEvent(pp, SummonEvent.Type.DIED);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		if (event.getType() == PrisonPearlEvent.Type.FREED) {
			summoned_pearls.remove(event.getPrisonPearl().getImprisonedName());
		}
	}
	
	private boolean summonEvent(PrisonPearl pp, SummonEvent.Type type) {
		return summonEvent(pp, type, null);
	}
	
	private boolean summonEvent(PrisonPearl pp, SummonEvent.Type type, Location loc) {
		SummonEvent event = new SummonEvent(pp, type, loc);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
}
