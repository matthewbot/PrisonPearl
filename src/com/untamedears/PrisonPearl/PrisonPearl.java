package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class PrisonPearl {
	private final short id;
	private final String imprisonedname;
    private String motd = "";
	private Player player;
	private Item item;
	private Location blocklocation;
	
	public PrisonPearl(short id, String imprisonedname, Player holderplayer) {
		this.id = id;
		this.imprisonedname = imprisonedname;
		this.player = holderplayer;
	}
	
	@SuppressWarnings("WeakerAccess")
    public PrisonPearl(short id, String imprisonedname, Location blocklocation) {
		this.id = id;
		this.imprisonedname = imprisonedname;
		this.blocklocation = blocklocation;
	}
	
	public static PrisonPearl makeFromLocation(short id, String imprisonedname, Location loc) {
		if (imprisonedname == null || loc == null)
			return null;
		BlockState bs = loc.getBlock().getState();
		if (bs instanceof InventoryHolder)
			return new PrisonPearl(id, imprisonedname, loc);
		else
			return null;
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
	
	public Player getHolderPlayer() {
		return player;
	}
	
	public BlockState getHolderBlockState() {
		if (blocklocation != null)
			return blocklocation.getBlock().getState();
		else
			return null;
	}
	
	public Item getHolderItem() {
		return item;
	}
	
	@SuppressWarnings("WeakerAccess")
    public String getHolderName() {
		if (player != null) {
			return player.getName();
		} else if (item != null) {
			return "nobody";
		} else if (blocklocation != null) {
			switch (getHolderBlockState().getType()) {
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
			System.err.println("PrisonPearl " + id + " has no player, item, nor location");
			return "unknown"; 
		}
	}
	
	public Location getLocation() {
		if (player != null) {
			return player.getLocation().add(0, -.5, 0);
		} else if (item != null) {
			return item.getLocation();
		} else if (blocklocation != null) {
			return blocklocation;
		} else {
			throw new RuntimeException("PrisonPearl " + id + " has no player, item, nor location");
		}
	}
	
	public String describeLocation() {
		Location loc = getLocation();
		Vector vec = loc.toVector();
		String str = loc.getWorld().getName() + " " + vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
		
		return "held by " + getHolderName() + " at " + str;
	}
	
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean verifyLocation() {
		if (item != null) {
			Chunk chunk = item.getLocation().getChunk();
			for (Entity entity : chunk.getEntities()) {
				if (entity == item)
					return true;
			}
			
			return false;
		} else {
			Inventory inv;
			if (player != null) {
				if (!player.isOnline())
					return false;
				ItemStack cursoritem = player.getItemOnCursor();
				if (cursoritem.getType() == Material.ENDER_PEARL && cursoritem.getDurability() == id)
					return true;
				inv = player.getInventory();
			} else if (blocklocation != null) {
				BlockState bs = getHolderBlockState();
				if (!(bs instanceof InventoryHolder))
					return false;
				inv = ((InventoryHolder)bs).getInventory();
				for (HumanEntity viewer : inv.getViewers()) {
					ItemStack cursoritem = viewer.getItemOnCursor();
					if (cursoritem.getType() == Material.ENDER_PEARL && cursoritem.getDurability() == id)
						return true;
				}
			} else {
				return false;
			}
				
			for (ItemStack item : inv.all(Material.ENDER_PEARL).values()) {
				if (item.getDurability() == id)
					return true;
			}
			return false;
		}
	}
	
	public void setHolder(Player player) {
		this.player = player;
		item = null;
		blocklocation = null;
	}
	
	public <ItemBlock extends BlockState & InventoryHolder> void setHolder(ItemBlock blockstate) {
		player = null;
		item = null;
		blocklocation = blockstate.getLocation();
	}
	
	public void setHolder(Item item) {
		player = null;
		this.item = item;
		blocklocation = null;
	}

    public String getMotd() {
        return motd;
    }

    public void setMotd(String motd) {
        this.motd = motd;
    }
}
