package com.untamedears.PrisonPearl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DamageLog {
	private String player;
	private Map<String, Integer> damagers;
	
	private long expirestick;
	
	public DamageLog(Player player) {
		this.player = player.getName();
		this.damagers = new HashMap<String, Integer>();
	}
	
	public String getName() {
		return player;
	}
	
	public Player getPlayer() {
		return Bukkit.getPlayer(player);
	}
	
	public int getDamage(Player player) {
		return getDamage(player.getName());
	}
	
	public int getDamage(String name) {
		Integer i = damagers.get(name);
		if (i == null)
			return 0;
		else
			return i;
	}
	
	public void recordDamage(Player damager, int damage, long expirestick) {
		Integer i = damagers.get(damager.getName());
		if (i == null)
			i = damage;
		else
			i += damage;
		damagers.put(damager.getName(), i);
		this.expirestick = expirestick;
	}
	
	public List<Player> getDamagers(int min) {
		List<Player> players = new ArrayList<Player>();
		
		for (Entry<String, Integer> entry : damagers.entrySet()) {
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player != null && entry.getValue() > min)
				players.add(player);
		}
		
		Collections.sort(players, new Comparator<Player>() {
			public int compare(Player p0, Player p1) {
				return Integer.compare(damagers.get(p0.getName()), damagers.get(p1.getName()));
			}
		});
		
		return players;
	}
	
	public long getExpiresTick() {
		return expirestick;
	}
}
