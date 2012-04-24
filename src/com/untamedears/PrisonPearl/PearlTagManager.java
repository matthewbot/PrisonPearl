package com.untamedears.PrisonPearl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class PearlTagManager implements Runnable, Listener {
	private PrisonPearlPlugin plugin;

	private boolean scheduled;
	private List<PearlTag> tags;
	
	public PearlTagManager(PrisonPearlPlugin plugin) {
		this.plugin = plugin;
		
		scheduled = false;
		tags = new ArrayList<PearlTag>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	// Create tags when somebody punches somebody else with a pearl
	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player))
			return;

		Player taggerplayer = (Player)event.getDamager();
		Player taggedplayer = (Player)event.getEntity();

		ItemStack item = taggerplayer.getItemInHand();
		if (item.getType() != Material.ENDER_PEARL || item.getDurability() != 0)
			return;
		
		// generate a tag and make a new tag event
		long expires = getNowTick() + plugin.getConfig().getInt("tag_ticks");
		PearlTag tag = new PearlTag(taggedplayer, taggerplayer, expires);
		if (!tagEvent(tag, PearlTagEvent.Type.NEW))
			return;
		
		// if player is already tagged, remove them and generate switched events
		eventTagsOnPlayer(taggedplayer, PearlTagEvent.Type.SWITCHED, taggerplayer, true);
		
		// then add the new tag and schedule the expire task
		tags.add(tag);
		scheduleExpireTask();
	}
	
	// Expire tags when a player logs off
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		expireTagsHeldByPlayer(player);
		eventTagsOnPlayer(player, PearlTagEvent.Type.EXPIRED, null, true);
	}
	
	// Expire tags when a player dies
	// Kill the tag he had on him
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player)event.getEntity();
		expireTagsHeldByPlayer(player);
		eventTagsOnPlayer(player, PearlTagEvent.Type.KILLED, null, true);
	}
	
	private void expireTagsHeldByPlayer(Player tagger) {
		Iterator<PearlTag> i = tags.iterator();
		while (i.hasNext()) {
			PearlTag tag = i.next();
			if (tag.getTaggerPlayer() == tagger) {
				i.remove();
				tagEvent(tag, PearlTagEvent.Type.EXPIRED);
			}
		}		
	}
	
	private void eventTagsOnPlayer(Player tagged, PearlTagEvent.Type type, Player other, boolean remove) {
		Iterator<PearlTag> i = tags.iterator();
		while (i.hasNext()) {
			PearlTag tag = i.next();
			if (tag.getTaggedPlayer() == tagged) {
				if (remove)
					i.remove();
				tagEvent(tag, type, other);
			}
		}
	}
	
	public void run() {
		scheduled = false;
		long nowtick = getNowTick();
		Iterator<PearlTag> i = tags.iterator();
		
		while (i.hasNext()) {
			PearlTag tag = i.next();
			if (tag.getTicksRemaining(nowtick) > 0)
				break;
			
			i.remove();
			tagEvent(tag, PearlTagEvent.Type.EXPIRED);
		}
		
		scheduleExpireTask();
	}
	
	private void scheduleExpireTask() {
		if (scheduled || tags.size() == 0)
			return;
		
		long remaining = tags.get(0).getTicksRemaining(getNowTick());
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, this, remaining+1);
		scheduled = true;
	}
	
	private long getNowTick() {
		return Bukkit.getWorlds().get(0).getFullTime();
	}
	
	private boolean tagEvent(PearlTag tag, PearlTagEvent.Type type) {
		PearlTagEvent event = new PearlTagEvent(tag, type, null);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
	
	private boolean tagEvent(PearlTag tag, PearlTagEvent.Type type, Player other) {
		PearlTagEvent event = new PearlTagEvent(tag, type, other);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
}
