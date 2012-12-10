package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PrisonPortaledPlayerManager implements Listener, SaveLoad {
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	
	private final Set<String> portaled_players;
	private boolean dirty;
	
	public PrisonPortaledPlayerManager(PrisonPearlPlugin plugin, PrisonPearlStorage pearls) {
		this.plugin = plugin;
		this.pearls = pearls;
		
		portaled_players = new HashSet<String>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void load(File file) throws NumberFormatException, IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String name;
		while ((name = br.readLine()) != null) {
			portaled_players.add(name);
		}
		
		fis.close();
		dirty = false;
	}
	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
		
		for (String name : portaled_players) {
			br.append(name).append("\n");
		}
		
		br.flush();
		fos.close();
		dirty = false;
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isPlayerPortaledToPrison(Player player) {
		return isPlayerPortaledToPrison(player.getName());
	}
	
	@SuppressWarnings("WeakerAccess")
    public boolean isPlayerPortaledToPrison(String playername) {
		return portaled_players.contains(playername);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (pearls.isImprisoned(player.getName()))
			return;
		
		if (event.getRespawnLocation().getWorld() != getPrisonWorld()) {
			portaled_players.remove(player.getName());
			dirty = true;
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerPortalEvent(PlayerPortalEvent event) {
		Player player = event.getPlayer();
		if (pearls.isImprisoned(player.getName()))
			return;
		
		if (event.getTo().getWorld() == getPrisonWorld())
			portaled_players.add(player.getName());
		else
			portaled_players.remove(player.getName());
		dirty = true;
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		if (event.getType() == PrisonPearlEvent.Type.NEW) {
			portaled_players.remove(event.getPrisonPearl().getImprisonedName());
			dirty = true;
		}
	}
	
	private World getPrisonWorld() {
		return Bukkit.getWorld(plugin.getConfig().getString("prison_world"));
	}
}
