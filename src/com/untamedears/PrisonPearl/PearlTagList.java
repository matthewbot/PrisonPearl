package com.untamedears.PrisonPearl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PearlTagList implements Runnable {
	private Plugin plugin;
	private long expiresticks;
	private boolean scheduled;
	private List<PearlTag> tags;
	
	public PearlTagList(Plugin plugin, long expiresticks) {
		this.plugin = plugin;
		this.expiresticks = expiresticks;
		scheduled = false;
		tags = new ArrayList<PearlTag>();
	}
	
	public void tag(Player taggedplayer, Player taggerplayer) {
		// first, generate a tag switch event if this person is being re-tagged by someone else
		Iterator<PearlTag> i = tags.iterator();
		while (i.hasNext()) {
			PearlTag tag = i.next();
			if (tag.getTaggedPlayer() == taggedplayer) {
				i.remove();
				tagEvent(tag, PearlTagEvent.Type.SWITCHED, taggerplayer);
				break;
			}
		}
		
		// then, make the tag and generate a new tag event
		long expires = getNowTick() + expiresticks;
		PearlTag tag = new PearlTag(taggedplayer, taggerplayer, expires);
		tags.add(tag);
		tagEvent(tag, PearlTagEvent.Type.NEW);
		
		// schedule the expire task
		scheduleExpireTask();
	}
	
	public PearlTag taggedKilled(Player taggedplayer) {
		Iterator<PearlTag> i = tags.iterator();
		while (i.hasNext()) {
			PearlTag tag = i.next();
			if (tag.getTaggedPlayer() == taggedplayer) {
				i.remove();
				tagEvent(tag, PearlTagEvent.Type.KILLED);
				return tag;
			}
		}
		
		return null;
	}
	
	public void taggerExpired(Player tagger) {
		Iterator<PearlTag> i = tags.iterator();
		while (i.hasNext()) {
			PearlTag tag = i.next();
			if (tag.getTaggerPlayer() == tagger) {
				i.remove();
				tagEvent(tag, PearlTagEvent.Type.EXPIRED);
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
	
	private void tagEvent(PearlTag tag, PearlTagEvent.Type type) {
		Bukkit.getPluginManager().callEvent(new PearlTagEvent(tag, type, null));
	}
	
	private void tagEvent(PearlTag tag, PearlTagEvent.Type type, Player other) {
		Bukkit.getPluginManager().callEvent(new PearlTagEvent(tag, type, other));
	}
}
