package com.untamedears.PrisonPearl;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PearlTagEvent extends Event {
	public enum Type { NEW, EXPIRED, IMPRISONED };
	
	private PearlTag tag;
	private Type type;
	
	public PearlTagEvent(PearlTag tag, Type type) {
		this.tag = tag;
		this.type = type;
	}
	
	public PearlTag getTag() {
		return tag;
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
