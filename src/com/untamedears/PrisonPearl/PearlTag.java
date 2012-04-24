package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PearlTag {
	private String taggedname;
	private String taggername;
	
	private long expirestick;
	
	public PearlTag(Player taggedplayer, Player taggerplayer, long expirestick) {
		taggedname = taggedplayer.getName();
		taggername = taggerplayer.getName();
		this.expirestick = expirestick;
	}
	
	public String getTaggedName() {
		return taggedname;
	}
	
	public Player getTaggedPlayer() {
		return Bukkit.getPlayer(taggedname);
	}
	
	public String getTaggerName() {
		return taggername;
	}
	
	public Player getTaggerPlayer() {
		return Bukkit.getPlayer(taggername);
	}
	
	public long getExpiresTick() {
		return expirestick;
	}
	
	public long getTicksRemaining(long nowtick) {
		return expirestick - nowtick;
	}
}
