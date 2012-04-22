package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;

public class PrisonPearl {
	private short id;
	private String imprisonedname;
	private Player holder;
	private Location storedat;
	
	public PrisonPearl(short id, String imprisonedname, Player holder) {
		this.id = id;
		this.imprisonedname = imprisonedname;
		this.holder = holder;
	}
	
	public PrisonPearl(short id, String imprisonedname, Location storedat) {
		this.id = id;
		this.imprisonedname = imprisonedname;
		this.storedat = storedat;
	}
	
	public PrisonPearl(String datastring) {
		String[] parts = datastring.split(" "); // id imprisoned worldname x y z
		
		id = Short.parseShort(parts[0]);
		imprisonedname = parts[1];
		storedat = new Location(Bukkit.getServer().getWorld(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
	}
	
	public String toDataString() {
		if (holder != null)
			throw new RuntimeException("PlayerPearl held by a player can't be turned into a datastring!");
		Location loc = storedat;
		return id + " " + imprisonedname + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ();
	}
	
	public short getID() {
		return id;
	}
	
	public void invalidate() {
		id = -1;
		imprisonedname = null;
		holder = null;
		storedat = null;
	}
	
	public boolean isValid() {
		return id != -1;
	}
	
	public Location getLocation() {
		if (holder != null) {
			return holder.getLocation();
		} else if (storedat != null) {
			return storedat;
		} else {
			return null;
		}
	}
	
	public String getImprisonedName() {
		return imprisonedname;
	}
	
	public Player getImprisonedPlayer() {
		return Bukkit.getPlayerExact(imprisonedname);
	}
	
	public Player getHolder() {
		return holder;
	}
	
	public boolean isHeld() {
		return holder != null;
	}
	
	public boolean isStored() {
		return storedat != null;
	}
	
	public void storeAt(Location loc, Server server) {
		holder = null;
		storedat = loc;
		
		pearlEvent(this, PrisonPearlEvent.Type.STORED);
	}
	
	public void pickupBy(Player player) {	
		storedat = null;
		holder = player;
		
		pearlEvent(this, PrisonPearlEvent.Type.HELD);
	}
	
	private void pearlEvent(PrisonPearl pp, PrisonPearlEvent.Type type) {
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, type));
	}
}
