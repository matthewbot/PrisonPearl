package com.untamedears.PrisonPearl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class PrisonPearlStorage implements SaveLoad {
	private final Map<Short, PrisonPearl> pearls_byid;
	private final Map<String, PrisonPearl> pearls_byimprisoned;
	private short nextid;
	
	private boolean dirty;
	
	public PrisonPearlStorage() {
		pearls_byid = new HashMap<Short, PrisonPearl>();
		pearls_byimprisoned = new HashMap<String, PrisonPearl>();
		nextid = 1;
	}
	
	public boolean isDirty() {
		return dirty;
	}
	
	public void markDirty() {
		dirty = true;
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
			PrisonPearl pp = PrisonPearl.makeFromLocation(id, imprisoned, loc);
			if (pp == null) {
				System.err.println("PrisonPearl for " + imprisoned + " didn't validate, so is now set free. Chunks and/or prisonpearls.txt are corrupt");
				continue;
			}
			
			addPearl(pp);
		}
		
		fis.close();
		
		dirty = false;
	}
	
	public void save(File file) throws IOException {
		FileOutputStream fos = new FileOutputStream(file);
		BufferedWriter br = new BufferedWriter(new OutputStreamWriter(fos));
	
		br.write(nextid + "\n");
		
		for (PrisonPearl pp : pearls_byid.values()) {
			if (pp.getHolderBlockState() == null)
				continue;
			
			Location loc = pp.getLocation();
            br.append((char) pp.getID());
            br.append(" ");
            br.append(pp.getImprisonedName());
            br.append(" ");
            br.append(loc.getWorld().getName());
            br.append(" ");
            br.append((char) loc.getBlockX());
            br.append(" ");
            br.append((char) loc.getBlockY());
            br.append(" ");
            br.append((char) loc.getBlockZ());
            br.append("\n");
        }
		
		br.flush();
		fos.close();
		
		dirty = false;
	}
	
	public PrisonPearl newPearl(Player imprisoned, Player imprisoner) {
		return newPearl(imprisoned.getName(), imprisoner);
	}
	
	public PrisonPearl newPearl(String imprisonedname, Player imprisoner) {
		PrisonPearl pp = new PrisonPearl(nextid++, imprisonedname, imprisoner);
		addPearl(pp);
		return pp;
	}
	
	public void deletePearl(PrisonPearl pp) {
		pearls_byid.remove(pp.getID());
		pearls_byimprisoned.remove(pp.getImprisonedName());
		dirty = true;
	}
	
	public void addPearl(PrisonPearl pp) {
		PrisonPearl old = pearls_byimprisoned.get(pp.getImprisonedName());
		if (old != null)
			pearls_byid.remove(old.getID());
		
		pearls_byid.put(pp.getID(), pp);
		pearls_byimprisoned.put(pp.getImprisonedName(), pp);
		dirty = true;
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
	
	boolean isImprisoned(String name) {
		return pearls_byimprisoned.containsKey(name);
	}
	
	boolean isImprisoned(Player player) {
		return pearls_byimprisoned.containsKey(player.getName());
	}
	
	public Integer getImprisonedCount(String[] names) {
		Integer count = 0;
        for (String name : names) {
            if (pearls_byimprisoned.containsKey(name)) {
                count++;
            }
        }
		return count;
	}
	
	public String[] getImprisonedNames(String[] names) {
		List<String> iNames = new ArrayList<String>();
        for (String name : names) {
            if (pearls_byimprisoned.containsKey(name)) {
                iNames.add(name);
            }
        }
		int count = iNames.size();
		String[] results = new String[count];
		for (int i = 0; i < count; i++) {
			results[i] = iNames.get(i);
		}
		return results;
	}
}
