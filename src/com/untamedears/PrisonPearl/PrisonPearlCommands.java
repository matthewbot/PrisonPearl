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
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.Furnace;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
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
            if (parts.length != 6) {
                String motd = "";
                for (int i = 6; i < parts.length; i++) {
                    motd = motd.concat(parts[i] + " ");
                }
                pp.setMotd(motd);
            }
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
            br.append(String.valueOf(pp.getID()));
            br.append(" ");
            br.append(pp.getImprisonedName());
            br.append(" ");
            br.append(loc.getWorld().getName());
            br.append(" ");
            br.append(String.valueOf(loc.getBlockX()));
            br.append(" ");
            br.append(String.valueOf(loc.getBlockY()));
            br.append(" ");
            br.append(String.valueOf(loc.getBlockZ()));
            br.append(" ");
            br.append(pp.getMotd());
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
	
	public Integer getPearlCount(){
		return pearls_byimprisoned.size();
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
	
	public String feedPearls(PrisonPearlManager pearlman){
		String message = "";
		ConcurrentHashMap<Short,PrisonPearl> map = new ConcurrentHashMap<Short,PrisonPearl>(pearls_byid);
		
		for (PrisonPearl pp : map.values()) {
			BlockState inherentViolence = pp.getHolderBlockState();
			Material mat = inherentViolence.getType();
			Inventory inv[] = new Inventory[2];
			inv[0] = inv[1] = null;
			if (inherentViolence == null)
			{
				continue;
			}
			else if (mat == Material.CHEST || mat == Material.LOCKED_CHEST){
				Chest c = ((Chest)inherentViolence);
				DoubleChestInventory dblInv = null;
				try{
					dblInv = (DoubleChestInventory)c.getInventory();
					inv[0] = dblInv.getLeftSide();
					inv[1] = dblInv.getRightSide();
				}
				catch(Exception e){
					inv[0] = (Inventory)c.getInventory();
				}
			}
			else if (mat == Material.FURNACE){
				Furnace f = ((Furnace)inherentViolence);
				inv[0] = f.getInventory();
			}
			else if (mat == Material.DISPENSER)
			{
				Dispenser d = ((Dispenser)inherentViolence);
				inv[0] = d.getInventory();
			}
			else if (mat == Material.BREWING_STAND) 
			{
				BrewingStand b = ((BrewingStand)inherentViolence);
				inv[0] = b.getInventory();
			}
			else
			{
				pearlman.freePearl(pp);
				continue;
			}
			
			message = message + " Pearl #" + pp.getID() + ", Name: " + pp.getImprisonedName() + " in a " + pp.getHolderBlockState().getType();
			int requirementSize = 8;
			ItemStack requirement = new ItemStack(Material.COAL, requirementSize);
			if(inv[0].containsAtLeast(requirement,requirementSize))
			{
				message = message + "\n Chest contains enough purestrain coal.";
				inv[0].removeItem(requirement);
			}
			else if(inv[1] != null && inv[1].containsAtLeast(requirement,requirementSize)){
				message = message + "\n Chest contains enough purestrain coal.";
				inv[1].removeItem(requirement);
			}
			else {
				message = message + "\n Chest does not contain enough purestrain coal.";
				pearlman.freePearl(pp);
			}
		}
		return message;
	}
}
