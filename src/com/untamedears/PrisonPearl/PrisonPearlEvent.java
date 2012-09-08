package com.untamedears.PrisonPearl;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PrisonPearlEvent extends Event {
	public enum Type { NEW, HELD, DROPPED, FREED }
	
	private PrisonPearl pp;
	private Type type;
	private Player imprisoner;
	
	private boolean cancelled;
	
	public PrisonPearlEvent(PrisonPearl pp, Type type) {
		this.pp = pp;
		this.type = type;
	}
	
	public PrisonPearlEvent(PrisonPearl pp, Type type, Player imprisoner) {
		this.pp = pp;
		this.type = type;
		this.imprisoner = imprisoner;
	}
	
	public PrisonPearl getPrisonPearl() {
		return pp;
	}
	
	public Type getType() {
		return type;
	}

	public Player getImprisoner() {
		return imprisoner;
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
