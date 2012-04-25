package com.untamedears.PrisonPearl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

public class DamageLogManager implements Runnable, Listener {
	private PrisonPearlPlugin plugin;

	private boolean scheduled;
	private Map<String, DamageLog> logs;
	
	public DamageLogManager(PrisonPearlPlugin plugin) {
		this.plugin = plugin;
		
		scheduled = false;
		logs = new HashMap<String, DamageLog>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public List<Player> getDamagers(Player player) {
		DamageLog log = logs.get(player.getName());
		if (log != null)
			return log.getDamagers(plugin.getConfig().getInt("damage_min"));
		else
			return null;
	}
	
	// Create damage logs
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player))
			return;
		
		Player player = (Player)event.getEntity();
		Player damager = (Player)event.getDamager();

		DamageLog log = logs.get(player.getName());
		if (log == null) {
			log = new DamageLog(player);
			logs.put(player.getName(), log);
		}
		
		long ticks = plugin.getConfig().getInt("damage_ticks");
		log.recordDamage(damager, event.getDamage(), getNowTick() + ticks);
		scheduleExpireTask(ticks);
	}
	
	// Reset damage logs on dead players
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		logs.remove(((Player)event.getEntity()).getName());
	}
	
	public void run() {
		scheduled = false;
		
		long nowtick = getNowTick();
		
		Iterator<DamageLog> i = logs.values().iterator();
		int ctr = 0;
		long minremaining = Long.MAX_VALUE;
		while (i.hasNext()) {
			DamageLog log = i.next();
			long remaining = nowtick-log.getExpiresTick();
			
			if (remaining <= plugin.getConfig().getInt("damage_ticks")/20) {
				i.remove();
				ctr++;
				continue;
			}

			minremaining = Math.min(minremaining, remaining);
		}
		
		if (minremaining < Long.MAX_VALUE)
			scheduleExpireTask(minremaining);
		
		System.out.println("Expired " + ctr + " damage logs");
	}
	
	private void scheduleExpireTask(long ticks) {
		if (scheduled)
			return;
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, ticks);
		scheduled = true;
	}
	
	private long getNowTick() {
		return Bukkit.getWorlds().get(0).getFullTime();
	}
}
