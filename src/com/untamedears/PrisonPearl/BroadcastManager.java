package com.untamedears.PrisonPearl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;

class BroadcastManager {
	private final Map<Player, Map<Player, Boolean>> broadcasts;
	private final Map<Player, Player> quickconfirm;
	
	public BroadcastManager() {
		broadcasts = new HashMap<Player, Map<Player, Boolean>>();
		quickconfirm = new HashMap<Player, Player>();
	}
	
	public boolean addBroadcast(Player player, Player receiver) {
		Map<Player, Boolean> receivers = broadcasts.get(player);
		if (receivers == null) {
			receivers = new HashMap<Player, Boolean>();
			broadcasts.put(player, receivers);
		} else {
			if (receivers.containsKey(receiver))
				return false;
		}
		
		receivers.put(receiver, false);
		quickconfirm.put(receiver, player);
		return true;
	}
	
	public boolean confirmBroadcast(Player player, Player receiver) {
		Map<Player, Boolean> receivers = broadcasts.get(player);
		if (receivers == null)
			return false;
		if (!receivers.containsKey(receiver))
			return false;
		receivers.put(receiver, true);
		quickconfirm.remove(receiver);
		return true;
	}
	
	public boolean silenceBroadcast(Player player, Player receiver) {
		Map<Player, Boolean> receivers = broadcasts.get(player);
        return receivers != null && receivers.remove(receiver) != null;
    }
	
	@SuppressWarnings("UnusedDeclaration")
    public boolean removeBroadcasts(Player player) {
		return broadcasts.remove(player) != null;
	}
	
	public Player getQuickConfirmPlayer(Player receiver) {
		return quickconfirm.get(receiver);
	}
	
	public void broadcast(Player player, String msg) {
		Map<Player, Boolean> receivers = broadcasts.get(player);
		if (receivers == null)
			return;
		
		Iterator<Entry<Player, Boolean>> i = receivers.entrySet().iterator();
		while (i.hasNext()) {
			Entry<Player, Boolean> entry = i.next();
			if (!entry.getKey().isOnline()) {
				i.remove();
				continue;
			}
			
			if (entry.getValue())
				entry.getKey().sendMessage(msg);
		}
	}
	
	@SuppressWarnings("UnusedDeclaration")
    @EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		broadcasts.remove(event.getPlayer());
		quickconfirm.remove(event.getPlayer());
	}
}
