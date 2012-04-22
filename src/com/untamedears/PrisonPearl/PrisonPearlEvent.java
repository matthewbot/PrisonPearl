package com.untamedears.PrisonPearl;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrisonPearlEvent extends Event {
	public enum Type { NEW, STORED, HELD, FREED };
	
	private PrisonPearl pp;
	private Type type;
	
	public PrisonPearlEvent(PrisonPearl pp, Type type) {
		this.pp = pp;
		this.type = type;
	}
	
	public PrisonPearl getPrisonPearl() {
		return pp;
	}
	
	public Type getType() {
		return type;
	}
	
	private static final HandlerList handlers = new HandlerList();
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
