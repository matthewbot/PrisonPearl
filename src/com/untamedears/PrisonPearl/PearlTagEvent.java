package com.untamedears.PrisonPearl;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PearlTagEvent extends Event {
	public enum Type { NEW, EXPIRED, SWITCHED, KILLED };
	
	private PearlTag tag;
	private Type type;
	private Player other;
	
	public PearlTagEvent(PearlTag tag, Type type, Player other) {
		this.tag = tag;
		this.type = type;
		this.other = other;
	}
	
	public PearlTag getTag() {
		return tag;
	}
	
	public Type getType() {
		return type;
	}
	
	public Player getOtherPlayer() {
		return other;
	}
	
	private static final HandlerList handlers = new HandlerList();
	public HandlerList getHandlers() {
	    return handlers;
	}
	public static HandlerList getHandlerList() {
	    return handlers;
	}
}
