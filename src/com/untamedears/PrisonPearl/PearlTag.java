package com.untamedears.PrisonPearl;

import org.bukkit.entity.Player;

public class PearlTag {
	private Player taggedplayer;
	private Player taggerplayer;
	
	private long expirestick;
	
	public PearlTag(Player taggedplayer, Player taggerplayer, long expirestick) {
		this.taggedplayer = taggedplayer;
		this.taggerplayer = taggerplayer;
		this.expirestick = expirestick;
	}
	
	public Player getTaggedPlayer() {
		return taggedplayer;
	}
	
	public Player getTaggerPlayer() {
		return taggerplayer;
	}
	
	public long getExpiresTick() {
		return expirestick;
	}
	
	public long getTicksRemaining(long nowtick) {
		return expirestick - nowtick;
	}
}
