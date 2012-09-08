package com.untamedears.PrisonPearl;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class SummonEvent extends Event {
	public enum Type { SUMMONED, RETURNED, KILLED, DIED }

    private PrisonPearl pp;
	private Type type;
	private Location location;
	
	private boolean cancelled;
	
	public SummonEvent(PrisonPearl pp, Type type) {
		this.pp = pp;
		this.type = type;
	}
	
	public SummonEvent(PrisonPearl pp, Type type, Location location) {
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

	public boolean isCancelled() {
		return cancelled;
	}
	
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	private static final HandlerList handlers = new HandlerList();
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
