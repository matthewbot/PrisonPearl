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
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

public class PrisonPearlStorage {
	private Map<Short, PrisonPearl> pearls_byid;
	private Map<String, PrisonPearl> pearls_byimprisoned;
	private short nextid;
	
	public PrisonPearlStorage() {
		pearls_byid = new HashMap<Short, PrisonPearl>();
		pearls_byimprisoned = new HashMap<String, PrisonPearl>();
		nextid = 1;
	}

	public void load(File file) throws IOException {
		FileInputStream fis = new FileInputStream(file);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		
		String line;
		while ((line = br.readLine()) != null) {
			String parts[] = line.split(" ");
			short id = Short.parseShort(parts[0]);
			String imprisoned = parts[1];
			Location loc = new Location(Bukkit.getWorld(parts[2]), Integer.parseInt(parts[3]), Integer.parseInt(parts[4]), Integer.parseInt(parts[5]));
			BlockState block = loc.getBlock().getState();
			if (!(block instanceof InventoryHolder))
				continue;
			
			PrisonPearl pp = new PrisonPearl(id, imprisoned, (InventoryHolder)block);
			put(pp);
			nextid = (short)(pp.getID()+1);
		}
		
		fis.close();
	}
	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
	
		for (PrisonPearl pp : pearls_byid.values()) {
			if (pp.getHolderBlockState() == null)
				continue;
			
			Location loc = pp.getHolderLocation();
			br.append(pp.getID() + " " + pp.getImprisonedName() + " " + loc.getWorld().getName() + " " + loc.getBlockX() + " " + loc.getBlockY() + " " + loc.getBlockZ() + "\n");
		}
		
		br.flush();
		fos.close();
	}
	
	public PrisonPearl imprison(Player imprisoned, Player imprisoner) {
		PrisonPearl pp = new PrisonPearl(nextid++, imprisoned.getName(), imprisoner);
		put(pp);
		pearlEvent(pp, PrisonPearlEvent.Type.NEW);
		return pp;
	}
	
	public void free(PrisonPearl pp, Location loc) {
		remove(pp);
		pearlEvent(pp, PrisonPearlEvent.Type.FREED, loc);
	}
	
	public PrisonPearl getByID(short id) {
		return pearls_byid.get(id);
	}
	
	public PrisonPearl getByImprisoned(String name) {
		return pearls_byimprisoned.get(name);
	}
	
	public PrisonPearl getByImprisoned(Player player) {
		return pearls_byimprisoned.get(player.getName());
	}
	
	private void put(PrisonPearl pp) {
		pearls_byid.put(pp.getID(), pp);
		pearls_byimprisoned.put(pp.getImprisonedName(), pp);
	}
	
	private void remove(PrisonPearl pp) {
		pearls_byid.remove(pp.getID());
		pearls_byimprisoned.remove(pp.getImprisonedName());
	}
	
	private void pearlEvent(PrisonPearl pp, PrisonPearlEvent.Type type) {
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, type));
	}
	
	private void pearlEvent(PrisonPearl pp, PrisonPearlEvent.Type type, Location loc) {
		Bukkit.getPluginManager().callEvent(new PrisonPearlEvent(pp, type, loc));
	}
}
