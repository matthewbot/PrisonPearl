package com.untamedears.PrisonPearl;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class SummonManager implements Runnable, Listener {
	private PrisonPearlPlugin plugin;
	private PrisonPearlStorage pearls;
	
	private Map<PrisonPearl, Location> summoned_pearls;
	
	public SummonManager(PrisonPearlPlugin plugin, PrisonPearlStorage pearls) {
		this.plugin = plugin;
		this.pearls = pearls;
		
		summoned_pearls = new HashMap<PrisonPearl, Location>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 0, plugin.getConfig().getInt("summon_damage_ticks"));
	}
	
	public void run() {
		for (PrisonPearl pp : summoned_pearls.keySet()) {
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
		if (player == null)
			return false;
		
		summoned_pearls.put(pp, player.getLocation());
		player.teleport(loc);
		summonEvent(pp, SummonEvent.Type.SUMMONED, loc);
		return true;
	}
	
	public boolean returnPearl(PrisonPearl pp) {
		Location loc = summoned_pearls.remove(pp);
		if (loc == null)
			return false;
		
		Player player = pp.getImprisonedPlayer();
		if (player != null)
			player.teleport(loc);
		summonEvent(pp, SummonEvent.Type.RETURNED, loc);
		return true;
	}
	
	public boolean killPearl(PrisonPearl pp) {
		if (summoned_pearls.remove(pp) == null)
			return false;
		
		pp.getImprisonedPlayer().setHealth(0);
		summonEvent(pp, SummonEvent.Type.KILLED);
		return true;
	}
	
	public boolean isSummoned(Player player) {
		PrisonPearl pp = pearls.getByImprisoned(player);
		if (pp == null)
			return false;
		
		return isSummoned(pp);
	}
	
	public boolean isSummoned(PrisonPearl pp) {
		return summoned_pearls.containsKey(pp);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		PrisonPearl pp = pearls.getByImprisoned((Player)event.getEntity());
		if (pp == null)
			return;
		
		summoned_pearls.remove(pp);
		summonEvent(pp, SummonEvent.Type.DIED);
	}
	
	private void summonEvent(PrisonPearl pp, SummonEvent.Type type) {
		Bukkit.getPluginManager().callEvent(new SummonEvent(pp, type, null));
	}
	
	private void summonEvent(PrisonPearl pp, SummonEvent.Type type, Location loc) {
		Bukkit.getPluginManager().callEvent(new SummonEvent(pp, type, loc));
	}
}
