package com.untamedears.PrisonPearl;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrisonPearlEvent extends Event {
	public enum Type { NEW, HELD, DROPPED, SUMMONED, RETURNED, FREED };
	
	private PrisonPearl pp;
	private Type type;
	private Location location;
	
	public PrisonPearlEvent(PrisonPearl pp, Type type) {
		this.pp = pp;
		this.type = type;
	}
	
	public PrisonPearlEvent(PrisonPearl pp, Type type, Location location) {
		this.pp = pp;
		this.type = type;
		this.location = location;
	}
	
	public PrisonPearl getPrisonPearl() {
		return pp;
	}
	
	public Type getType() {
		return type;
	}
	
	public Location getLocation() {
		return location;
	}
	
	private static final HandlerList handlers = new HandlerList();
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
