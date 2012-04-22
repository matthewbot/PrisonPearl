package com.untamedears.PrisonPearl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

public class PrisonPearlPlugin extends JavaPlugin implements Listener {
	private PearlTagList taglist;
	private PrisonPearlStorage pearlstorage;

	private Location prisonlocation;

	public void onEnable() {
		taglist = new PearlTagList(this, 20*10);
		pearlstorage = new PrisonPearlStorage();

		File ppfile = getPrisonPearlsFile();
		try {
			pearlstorage.load(ppfile);
		} catch (FileNotFoundException e) {
			System.out.println("Prison pearls data file does not exist, creating.");
			if (!ppfile.getParentFile().mkdirs()) {
				throw new RuntimeException("Failed to create directory " + ppfile.getParentFile().getAbsolutePath());
			}

			try {
				ppfile.createNewFile();
			} catch (IOException e2) {
				throw new RuntimeException("Failed to create " + ppfile.getAbsolutePath(), e2);
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to load prison pearls from " + ppfile.getAbsolutePath(), e);
		}

		List<World> worlds = getServer().getWorlds();
		prisonlocation = worlds.get(worlds.size()-1).getSpawnLocation();

		try {
			Method method = net.minecraft.server.Item.class.getDeclaredMethod("a", boolean.class);
			if (method.getReturnType() == net.minecraft.server.Item.class) {
				method.setAccessible(true);
				method.invoke(net.minecraft.server.Item.ENDER_PEARL, true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		getServer().getPluginManager().registerEvents(this, this);
	}

	public void onDisable() {
		try {
			File file = getPrisonPearlsFile();
			if (file.exists())
				file.renameTo(new File(file.getAbsolutePath() + ".bak"));
			pearlstorage.save(getPrisonPearlsFile());
		} catch (IOException e) {
			throw new RuntimeException("Failed to save prison pearls to " + getPrisonPearlsFile().getAbsolutePath(), e);
		}
	}

	private File getPrisonPearlsFile() {
		return new File(getDataFolder(), "prisonpearls.txt");
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onItemHeldChange(PlayerItemHeldEvent event) {
		ItemStack item = event.getPlayer().getInventory().getItem(event.getNewSlot());
		if (item == null)
			return;

		if (item.getType() == Material.ENDER_PEARL && item.getDurability() != 0) {
			Player player = event.getPlayer();
			PrisonPearl pp = pearlstorage.getByID(item.getDurability());

			if (pp != null) {
				player.sendMessage("Prison Pearl - " + pp.getImprisonedName());
			} else {
				item.setDurability((short)0);
				player.getInventory().setItem(event.getNewSlot(), item);	
			}
		}
	}

	@EventHandler(priority=EventPriority.NORMAL)
	public void onInventoryClick(InventoryClickEvent event) {
		ItemStack clicked = event.getCurrentItem();
		if (clicked != null && clicked.getType() == Material.ENDER_PEARL && clicked.getDurability() != 0) {
			PrisonPearl pp = pearlstorage.getByID(clicked.getDurability());
			if (pp == null) {
				clicked.setDurability((short)0);
				event.setCurrentItem(clicked);
			} else {
				if (event.getWhoClicked() instanceof Player)
					((Player)event.getWhoClicked()).sendMessage("Prison Pearl - " + pp.getImprisonedName());
			}
		}

		ItemStack item = event.getCursor();
		if (item.getType() != Material.ENDER_PEARL || item.getDurability() == 0)
			return;
		PrisonPearl pp = pearlstorage.getByID(item.getDurability());
		if (pp == null) {
			item.setDurability((short)0);
			event.setCursor(item);
			return;
		}

		InventoryView view = event.getView();
		int rawslot = event.getRawSlot();
		InventoryHolder holder;
		if (view.convertSlot(rawslot) == rawslot) { // this means in the top inventory
			holder = view.getTopInventory().getHolder();
		} else {
			holder = view.getBottomInventory().getHolder();
		}

		pp.setHolder(holder);
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		taglist.taggerExpired(event.getPlayer());

		PearlTag tag = taglist.taggedKilled(event.getPlayer());
		if (tag != null) {
			imprisonPlayer(tag.getTaggedPlayer(), tag.getTaggerPlayer());
		}

		Inventory inv = event.getPlayer().getInventory();
		for (Entry<Integer, ? extends ItemStack> entry : inv.all(Material.ENDER_PEARL).entrySet()) {
			int slot = entry.getKey();
			ItemStack item = entry.getValue();
			if (item.getDurability() == 0)
				continue;

			PrisonPearl pp = pearlstorage.getByID(item.getDurability());
			if (pp == null)
				continue;

			pearlstorage.free(pp, event.getPlayer().getLocation());
			inv.setItem(slot, null);
		}
	}

	@EventHandler(priority=EventPriority.HIGH)
	public void onEntityDeath(EntityDeathEvent event) {
		// only care about players
		if (!(event.getEntity() instanceof Player))
			return;

		// first, expire any tags the player might have had
		Player player = (Player)event.getEntity();
		taglist.taggerExpired(player);

		// then expire the tag on the player, and see if he should be imprisoned
		PearlTag tag = taglist.taggedKilled(player);
		if (tag != null)
			imprisonPlayer(tag.getTaggedPlayer(), tag.getTaggerPlayer());
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onItemDespawn(ItemDespawnEvent event) {
		ItemStack item = event.getEntity().getItemStack();
		if (item.getType() != Material.ENDER_PEARL || item.getDurability() == 0)
			return;

		PrisonPearl pp = pearlstorage.getByID(item.getDurability());
		if (pp == null)
			return;

		pearlstorage.free(pp, event.getEntity().getLocation());
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onChunkUnload(ChunkUnloadEvent event) {
		for (Entity e : event.getChunk().getEntities()) {
			if (!(e instanceof Item))
				continue;

			ItemStack item = ((Item)e).getItemStack();
			if (item.getType() != Material.ENDER_PEARL || item.getDurability() == 0)
				continue;

			final PrisonPearl pp = pearlstorage.getByID(item.getDurability());
			if (pp == null)
				continue;

			final Entity entity = e;
			Bukkit.getScheduler().callSyncMethod(this, new Callable<Void>() { // doing this in onChunkUnload causes weird things to happen
				public Void call() throws Exception {
					pearlstorage.free(pp, entity.getLocation());
					entity.remove();
					return null;
				}	
			});

			event.setCancelled(true);
		}
	}

	@EventHandler(priority=EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (pearlstorage.getByImprisoned(player) != null) {
			player.sendMessage("Your prison pearl has bound you to this bleak and endless world");
			event.setRespawnLocation(prisonlocation);
		} else if (event.getRespawnLocation().getWorld() == prisonlocation) {
			event.setRespawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation());
			player.sendMessage("You've been freed!");
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (!(event.getDamager() instanceof Player && event.getEntity() instanceof Player))
			return;

		Player damager = (Player)event.getDamager();
		Player player = (Player)event.getEntity();

		if (damager.getItemInHand().getType() == Material.ENDER_PEARL) {
			taglist.tag(player, damager);
		}
	}

	@EventHandler(priority=EventPriority.LOW)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		ItemStack item = event.getItem();
		if (item == null || item.getType() != Material.ENDER_PEARL || item.getDurability() == 0)
			return;

		PrisonPearl pp = pearlstorage.getByID(item.getDurability());
		if (pp == null)
			return;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Material m = event.getClickedBlock().getType();
			if (m == Material.CHEST || m == Material.WORKBENCH || m == Material.FURNACE || m == Material.DISPENSER || m == Material.BREWING_STAND) {
				return;
			}
		} else if (event.getAction() != Action.RIGHT_CLICK_AIR) {
			return;
		}

		pearlstorage.free(pp, player.getLocation());
		player.getInventory().setItemInHand(null);
		event.setCancelled(true);
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onEntityCombustEvent(EntityCombustEvent event) {
		if (!(event.getEntity() instanceof Item))
			return;

		ItemStack item = ((Item)event.getEntity()).getItemStack();
		if (item.getType() != Material.ENDER_PEARL || item.getDurability() == 0)
			return;

		PrisonPearl pp = pearlstorage.getByID(item.getDurability());
		if (pp == null)
			return;

		pearlstorage.free(pp, event.getEntity().getLocation());
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPearlTagEvent(PearlTagEvent event) {
		Player tagger = event.getTag().getTaggerPlayer();
		Player tagged = event.getTag().getTaggedPlayer();
		Player other = event.getOtherPlayer();

		if (event.getType() == PearlTagEvent.Type.NEW) {
			tagger.sendMessage("You've tagged " + tagged.getDisplayName());
			tagged.sendMessage("You've been tagged by " + tagger.getDisplayName());
		} else if (event.getType() == PearlTagEvent.Type.EXPIRED) {
			tagger.sendMessage("Your tag expired for " + tagged.getDisplayName());
			tagged.sendMessage("You are no longer tagged by " + tagger.getDisplayName());
		} else if (event.getType() == PearlTagEvent.Type.SWITCHED) {
			tagger.sendMessage("Your tag for " + tagged.getDisplayName() + " is now owned by " + other.getDisplayName());
		} else if (event.getType() == PearlTagEvent.Type.KILLED){
			tagger.sendMessage("You've bound " + tagged.getDisplayName() + " to a prison pearl!");
			tagged.sendMessage("You've been bound to a prison pearl owned by " + tagger.getDisplayName());
		}
	}

	@EventHandler(priority=EventPriority.MONITOR)
	public void onPrisonPearlEvent(PrisonPearlEvent event) {
		PrisonPearl pp = event.getPrisonPearl();
		Player player = pp.getImprisonedPlayer();
		if (player == null) // player not online?
			return; // gets no intel then

		switch (event.getType()) {
		case HELD:
			String world = pp.getHolderLocation().getWorld().getName();
			Vector vec = pp.getHolderLocation().toVector();
			String vecstr = vec.getBlockX() + " " + vec.getBlockY() + " " + vec.getBlockZ();
			player.sendMessage("Your prison pearl is now held by " + pp.getHolderName() + " at " + world + " " + vecstr);
			break;

		case FREED:
			player.sendMessage("You've been freed!");
			player.teleport(event.getLocation());
			break;
		}
	}

	private boolean imprisonPlayer(Player player, Player imprisoner) {
		// set up the imprisoner's inventory
		PlayerInventory inv = imprisoner.getInventory();
		int stacknum = inv.first(Material.ENDER_PEARL);
		if (stacknum == -1)
			return false; // imprisoner doesn't have pearl any more, so no go
		ItemStack stack = inv.getItem(stacknum);
		int pearlnum;
		if (stack.getAmount() == 1) { // if he's just got one pearl
			pearlnum = stacknum; // put the prison pearl there
		} else {
			pearlnum = inv.firstEmpty(); // otherwise, put the prison pearl in the first empty slot
			if (pearlnum > 0) {
				stack.setAmount(stack.getAmount()-1); // and reduce his stack of pearls by one
				inv.setItem(stacknum, stack);
			} else { // no empty slot?
				pearlnum = stacknum; // then overwrite his stack of pearls
			}
		}

		PrisonPearl pp = pearlstorage.imprison(player, imprisoner); // create the prison pearl
		inv.setItem(pearlnum, new ItemStack(Material.ENDER_PEARL, 1, pp.getID())); // give it to the imprisoner

		player.setBedSpawnLocation(Bukkit.getWorlds().get(0).getSpawnLocation()); // reset the player's normal spawn location
		return true;
	}
}
