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
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

public class PrisonPearlStorage implements Runnable {
	private Map<Short, PrisonPearl> pearls_byid;
	private Map<String, PrisonPearl> pearls_byimprisoned;
	private Set<PrisonPearl> pearls_summoning;
	private short nextid;
	
	public PrisonPearlStorage(Plugin plugin, long summondamageticks) {
		pearls_byid = new HashMap<Short, PrisonPearl>();
		pearls_byimprisoned = new HashMap<String, PrisonPearl>();
		pearls_summoning = new HashSet<PrisonPearl>();
		nextid = 1;
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, summondamageticks, summondamageticks);
	}
	
	public void run() {
		for (PrisonPearl pp : pearls_summoning) {
			Player player = pp.getImprisonedPlayer();
			if (player == null)
				continue;
			
			double dist = player.getLocation().distance(pp.getLocation());
			if (dist > 20) { // TODO configurable
				player.sendMessage("You feel the distant tug of your prison pearl");
				player.damage(1);
			}
		}
	}

	public void load(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		nextid = Short.parseShort(br.readLine());
		
		String line;
		while ((line = br.readLine()) != null) {
			String parts[] = line.split(" ");
			short id = Short.parseShort(parts[0]);
			String imprisoned = parts[1];
			Location loc = new Location(Bukkit.getWorld(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
			boolean summoning = parts[6].equals("S");
			BlockState block = loc.getBlock().getState();
			if (!(block instanceof InventoryHolder))
				continue;
			
			PrisonPearl pp = new PrisonPearl(id, imprisoned, (InventoryHolder)block);
			put(pp);
			if (summoning)
				setSummoning(pp, true);
		}
		
		fis.close();
	}
	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
	
		br.write(nextid + "\n");
		
		for (PrisonPearl pp : pearls_byid.values()) {
			if (pp.getHolderBlockState() == null)
				continue;
			
			Location loc = pp.getLocation();
			br.append(pp.getID() + " " + pp.getImprisonedName() + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + " " + (isSummoning(pp) ? "S" : "-") + "\n");
		}
		
		br.flush();
		fos.close();
	}
	
	public PrisonPearl imprison(Player imprisoned, Player imprisoner) {
		PrisonPearl pp = new PrisonPearl(nextid++, imprisoned.getName(), imprisoner);
		put(pp);
		return pp;
	}
	
	private void put(PrisonPearl pp) {
		pearls_byid.put(pp.getID(), pp);
		pearls_byimprisoned.put(pp.getImprisonedName(), pp);
	}
	
	public void free(PrisonPearl pp) {
		pearls_byid.remove(pp.getID());
		pearls_byimprisoned.remove(pp.getImprisonedName());
	}
	
	public void setSummoning(PrisonPearl pp, boolean summoning) {
		if (summoning)
			pearls_summoning.add(pp);
		else
			pearls_summoning.remove(pp);
	}
	
	public boolean isSummoning(PrisonPearl pp) {
		return pearls_summoning.contains(pp);
	}
	
	public PrisonPearl getByID(short id) {
		return pearls_byid.get(id);
	}
	
	public PrisonPearl getByItemStack(ItemStack item) {
		if (item == null || item.getType() != Material.ENDER_PEARL || item.getDurability() == 0)
			return null;
		else
			return pearls_byid.get(item.getDurability());
	}
	
	public PrisonPearl getByImprisoned(String name) {
		return pearls_byimprisoned.get(name);
	}
	
	public PrisonPearl getByImprisoned(Player player) {
		return pearls_byimprisoned.get(player.getName());
	}
}
