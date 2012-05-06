package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Summon {
	private String summonedname;
	private Location returnloc;
	private int alloweddistance;
	
	public Summon(String summonedname, Location returnloc, int alloweddistance) {
		this.summonedname = summonedname;
		this.returnloc = returnloc;
		this.alloweddistance = alloweddistance;
	}
	
	public String getSummonedName() {
		return summonedname;
	}
	
	public Player getSummonedPlayer() {
		return Bukkit.getPlayerExact(summonedname);
	}
	
	public Location getReturnLocation() {
		return returnloc;
	}
	
	public int getAllowedDistance() {
		return alloweddistance;
	}
	
	public void setAllowedDistance(int alloweddistance) {
		this.alloweddistance = alloweddistance;
	}
}
