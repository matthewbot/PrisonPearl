package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

public class SummonManager implements Listener, SaveLoad {
	private final PrisonPearlPlugin plugin;
	private final PrisonPearlStorage pearls;
	
	private final Map<String, Summon> summons;
	private boolean dirty;
	
	public SummonManager(PrisonPearlPlugin plugin, PrisonPearlStorage pearls) {
		this.plugin = plugin;
		this.pearls = pearls;
		
		summons = new HashMap<String, Summon>();
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
			public void run() {
				inflictSummonDamage();
			}
		}, 0, plugin.getConfig().getInt("summon_damage_ticks"));
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void load(File file) throws NumberFormatException, IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String line;
		while ((line = br.readLine()) != null) {
			String[] parts = line.split(" ");
			String name = parts[0];
			Location loc = new Location(Bukkit.getWorld(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
			int dist = parts.length == 6 ? Integer.parseInt(parts[5]) : plugin.getConfig().getInt("summon_damage_radius");
            int damage = parts.length == 7 ? Integer.parseInt(parts[6]) : plugin.getConfig().getInt("summon_damage_amt");
            boolean canSpeak = parts.length != 8 || Boolean.parseBoolean(parts[7]);
            boolean canDamage = parts.length != 9 || Boolean.parseBoolean(parts[8]);
            boolean canBreak = parts.length != 10 || Boolean.parseBoolean(parts[9]);

			
			if (!pearls.isImprisoned(name))
				continue;
			
			summons.put(name, new Summon(name, loc, dist, damage, canSpeak, canDamage, canBreak));
		}
		
		fis.close();
		dirty = false;
	}
	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
		
		for (Entry<String, Summon> entry : summons.entrySet()) {
			Summon summon = entry.getValue();
			Location loc = summon.getReturnLocation();
			br.append(summon.getSummonedName()).append(" ").append(loc.getWorld().getName()).append(" ").append((char) loc.getBlockX()).append(" ").append((char) loc.getBlockY()).append(" ").append((char) loc.getBlockZ()).append(" ").append((char) summon.getAllowedDistance()).append((char) summon.getDamageAmount()).append(String.valueOf(summon.isCanSpeak())).append(String.valueOf(summon.isCanDealDamage())).append(String.valueOf(summon.isCanBreakBlocks())).append("\n");
		}
		
		br.flush();
		fos.close();
		dirty = false;
	}
	
	private void inflictSummonDamage() {
		Iterator<Entry<String, Summon>> i = summons.entrySet().iterator();
		while (i.hasNext()) {
			Summon summon = i.next().getValue();
			PrisonPearl pp = pearls.getByImprisoned(summon.getSummonedName());
			if (pp == null) {
				System.err.println("Somehow " + summon.getSummonedName() + " was summoned but isn't imprisoned");
				i.remove();
				dirty = true;
				continue;
			}
			
			Player player = pp.getImprisonedPlayer();
			if (player == null)
				continue;
			
			Location pploc = pp.getLocation();
			Location playerloc = player.getLocation();
			
			if (pploc.getWorld() != playerloc.getWorld() || pploc.distance(playerloc) > summon.getAllowedDistance())
				player.damage(summon.getDamageAmount());
		}
	}
	
	public boolean summonPearl(PrisonPearl pp) {
		Player player = pp.getImprisonedPlayer();
		if (player == null || player.isDead())
			return false;
		
		if (summons.containsKey(player.getName()))
			return false;
		
		Summon summon = new Summon(player.getName(), player.getLocation().add(0, -.5, 0), plugin.getConfig().getInt("summon_damage_radius"), plugin.getConfig().getInt("summon_damage_amt"), true, true, true);
		summons.put(summon.getSummonedName(), summon);
		
		if (!summonEvent(pp, SummonEvent.Type.SUMMONED, pp.getLocation())) {
			summons.remove(player.getName());
			return false;
		}
		
		dirty = true;
		return true;
	}
	
	public boolean returnPearl(PrisonPearl pp) {
		Summon summon = summons.remove(pp.getImprisonedName());
		if (summon == null)
			return false;
		
		if (!summonEvent(pp, SummonEvent.Type.RETURNED, summon.getReturnLocation())) {
			summons.put(pp.getImprisonedName(), summon);
			return false;
		}
		
		dirty = true;
		return true;
	}
	
	public boolean killPearl(PrisonPearl pp) {
		Summon summon = summons.remove(pp.getImprisonedName());
		if (summon == null)
			return false;
		
		if (!summonEvent(pp, SummonEvent.Type.KILLED, summon.getReturnLocation())) {
			summons.put(pp.getImprisonedName(), summon);
			return false;
		}
		
		pp.getImprisonedPlayer().setHealth(0);
		dirty = true;
		return true;
	}
	
	public boolean isSummoned(Player player) {
		return summons.containsKey(player.getName());
	}
	
	public boolean isSummoned(PrisonPearl pp) {
		return summons.containsKey(pp.getImprisonedName());
	}
	
	public Summon getSummon(Player player) {
		return summons.get(player.getName());
	}
	
	public Summon getSummon(String name) {
		return summons.get(name);
	}
	
	@EventHandler(priority=EventPriority.HIGHEST)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!(event.getEntity() instanceof Player))
			return;

		Player player = (Player)event.getEntity();
		Summon summon = summons.remove(player.getName());
		if (summon == null)
			return;
		dirty = true;
		
		PrisonPearl pp = pearls.getByImprisoned(player);
		if (pp == null)
			return;
		
		summonEvent(pp, SummonEvent.Type.DIED);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		if (event.getType() == PrisonPearlEvent.Type.FREED) {
			summons.remove(event.getPrisonPearl().getImprisonedName());
			dirty = true;
		}
	}
	
	@SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    private boolean summonEvent(PrisonPearl pp, @SuppressWarnings("SameParameterValue") SummonEvent.Type type) {
		SummonEvent event = new SummonEvent(pp, type);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean summonEvent(PrisonPearl pp, SummonEvent.Type type, Location loc) {
		SummonEvent event = new SummonEvent(pp, type, loc);
		Bukkit.getPluginManager().callEvent(event);
		return !event.isCancelled();
	}
}
