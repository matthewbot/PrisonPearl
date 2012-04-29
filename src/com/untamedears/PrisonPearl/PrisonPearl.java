package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PrisonPearl {
	private short id;
	private String imprisonedname;
	private InventoryHolder holder;
	private Item item;
	
	public PrisonPearl(short id, String imprisonedname, InventoryHolder holder) {
		this.id = id;
		this.imprisonedname = imprisonedname;
		this.holder = holder;
	}
	
	public short getID() {
		return id;
	}
	
	public String getImprisonedName() {
		return imprisonedname;
	}
	
	public Player getImprisonedPlayer() {
		return Bukkit.getPlayerExact(imprisonedname);
	}
	
	public InventoryHolder getHolder() {
		return holder;
	}
	
	public Entity getHolderEntity() {
		if (holder instanceof Entity)
			return (Entity)holder;
		else
			return null;
	}
	
	public BlockState getHolderBlockState() {
		if (holder instanceof BlockState) {
			return (BlockState)holder;
		} else if (holder instanceof DoubleChest) {
			return (BlockState)((DoubleChest)holder).getLeftSide();
		} else {
			return null;
		}
	}
	
	public Location getLocation() {
		if (holder != null) {
			if (holder instanceof Entity) {
				return ((Entity)holder).getLocation();
			} else if (holder instanceof BlockState) {
				return ((BlockState)holder).getLocation();
			} else if (holder instanceof DoubleChest) {
				return ((DoubleChest)holder).getLocation();
			} else {
				System.err.println("PrisonPearl " + id + " has an unexpected holder: " + holder);
				return null;
			}
		} else if (item != null) {
			return item.getLocation();
		} else {
			System.err.println("PrisonPearl " + id + " has no holder nor item");
			return null;
		}
	}
	
	public String getHolderName() {
		Entity entity;
		BlockState state;
		if ((entity = getHolderEntity()) != null) {
			if (entity instanceof Player) {
				return ((Player)entity).getName();
			} else {
				System.err.println("PrisonPearl " + id + " is held by a non-player entity");
				return "an unknown entity";
			}
		} else if ((state = getHolderBlockState()) != null) {
			switch (state.getType()) {
			case CHEST:
				return "a chest";
			case FURNACE:
				return "a furnace";
			case BREWING_STAND:
				return "a brewing stand";
			case DISPENSER:
				return "a dispenser";
			default:
				System.err.println("PrisonPearl " + id + " is inside an unknown block");
				return "an unknown block"; 
			}
		} else {
			System.err.println("PrisonPearl " + id + " has no holder nor item");
			return "unknown"; 
		}
	}
	
	public String describeLocation() {
		Location loc = getLocation();
		Vector vec = loc.toVector();
		String str = loc.getWorld().getName() + " " + vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
		
		if (holder != null)
			return "held by " + getHolderName() + " at " + str;
		else
			return "located at " + str;
	}
	
	public boolean verifyLocation() {
		Location loc = getLocation();
		
		if (item != null) {
			Chunk chunk = loc.getChunk();
			for (Entity entity : chunk.getEntities()) {
				if (entity == item)
					return true;
			}
			
			return false;
		} else {
			if (holder instanceof Player) {
				if (!((Player)holder).isOnline())
					return false;
			} else if (holder instanceof BlockState) {
				if (!((BlockState)holder).update(true))
					return false;
			} else if (holder instanceof DoubleChest) {
				DoubleChest dc = (DoubleChest)holder;
				if (!((BlockState)dc.getLeftSide()).update(true))
					return false;
			}
				
			Inventory inv = holder.getInventory();
			for (ItemStack item : inv.all(Material.ENDER_PEARL).values()) {
				if (item.getDurability() == id)
					return true;
			}
			return false;
		}
	}
	
	public void setHolder(InventoryHolder holder) {
		this.holder = holder;
		item = null;
	}
	
	public void setItem(Item item) {
		holder = null;
		this.item = item;
	}
}
