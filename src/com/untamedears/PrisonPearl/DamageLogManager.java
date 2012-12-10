package com.untamedears.PrisonPearl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

class DamageLogManager implements Runnable, Listener {
	private final PrisonPearlPlugin plugin;

	private boolean scheduled;
	private final Map<String, DamageLog> logs;
	
	public DamageLogManager(PrisonPearlPlugin plugin) {
		this.plugin = plugin;
		
		scheduled = false;
		logs = new HashMap<String, DamageLog>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public List<Player> getDamagers(Player player) {
		DamageLog log = logs.get(player.getName());
		if (log != null)
			return log.getDamagers(plugin.getConfig().getInt("damagelog_min"));
		else
			return new ArrayList<Player>();
	}
	
	public boolean hasDamageLog(Player player) {
		return logs.containsKey(player.getName());
	}
	
	// Create damage logs
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;
		Player player = (Player)event.getEntity();
		
		Player damager = null;
		if (event.getDamager() instanceof Player) {
			damager = (Player)event.getDamager();
		} else if (event.getDamager() instanceof Wolf) {
			Wolf wolf = (Wolf)event.getDamager();
			if (wolf.getOwner() instanceof Player)
				damager = (Player)wolf.getOwner();
		} else if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow)event.getDamager();
			if (!(arrow.getShooter() instanceof Player))
				return;
			
			damager = (Player)arrow.getShooter();
		}
		
		if (damager == null || damager == player)
			return;
		
		recordDamage(player, damager, event.getDamage());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPotionSplashEvent(PotionSplashEvent event) {
		LivingEntity shooter = event.getPotion().getShooter();
		if (!(shooter instanceof Player))
			return;
		Player damager = (Player)shooter;
		
		// So, the idea here is because we can't really determine how much damage a potion actually caused
		// somebody (like poison, weakness, or the API doesn't even seem to tell you the difference between harm I and harm II),
		// we just award 6 damage points to the thrower as long as the potion is sufficiently bad.
		int damage = 6;
		
		boolean badpotion=false;
		for (PotionEffect effect : event.getPotion().getEffects()) {
			// apparently these aren't really enums, because == doesn't work
			if (effect.getType().equals(PotionEffectType.HARM) || effect.getType().equals(PotionEffectType.POISON) || effect.getType().equals(PotionEffectType.WEAKNESS)) {
				badpotion = true;
				break;
			}
		}
		
		if (!badpotion) // don't award damage for helpful or do-nothing potions, to prevent pearl stealing
			return;
		
		for (Entity entity : event.getAffectedEntities()) {
			if (!(entity instanceof Player))
				continue;
			
			recordDamage((Player)entity, damager, damage);
		}
	}
	
	private void recordDamage(Player player, Player damager, int amt) {
		DamageLog log = logs.get(player.getName());
		if (log == null) {
			log = new DamageLog(player);
			logs.put(player.getName(), log);
		}
		
		long ticks = plugin.getConfig().getInt("damagelog_ticks");
		log.recordDamage(damager, amt, getNowTick() + ticks);
		scheduleExpireTask(ticks);
	}
	
	// Reset damage logs on dead players
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		logs.remove(((Player)event.getEntity()).getName());
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		logs.remove(event.getPlayer().getName());
	}
	
	public void run() {
		scheduled = false;
		
		long nowtick = getNowTick();
		
		Iterator<DamageLog> i = logs.values().iterator();
		long minremaining = Long.MAX_VALUE;
		while (i.hasNext()) {
			DamageLog log = i.next();
			long remaining = nowtick-log.getExpiresTick();
			
			if (remaining <= plugin.getConfig().getInt("damagelog_ticks")/20) {
				i.remove();
				continue;
			}

			minremaining = Math.min(minremaining, remaining);
		}
		
		if (minremaining < Long.MAX_VALUE)
			scheduleExpireTask(minremaining);
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
