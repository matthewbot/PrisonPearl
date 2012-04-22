package com.untamedears.PrisonPearl;

import java.lang.reflect.Method;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

public class PrisonPearlPlugin extends JavaPlugin implements Listener {
	private PearlTagList taglist;
	private PrisonPearlStorage pearlstorage;
	
	private Location prisonlocation;
	
	public void onEnable() {
		taglist = new PearlTagList(this, 20*10);
		pearlstorage = new PrisonPearlStorage();
		
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
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		taglist.taggerExpired(event.getPlayer());
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
		if (tag == null) // no tag on the player, don't imprison him
			return;
		
		// set up the imprisoner's inventory
		Player imprisoner = tag.getTaggerPlayer();
		PlayerInventory inv = imprisoner.getInventory();
		int stacknum = inv.first(Material.ENDER_PEARL);
		if (stacknum == -1)
			return; // imprisoner doesn't have pearl anymore, so no go
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
			
		PrisonPearl pp = pearlstorage.imprison(tag.getTaggerPlayer(), tag.getTaggedPlayer()); // create the prison pearl
		inv.setItem(pearlnum, new ItemStack(Material.ENDER_PEARL, 1, pp.getID())); // give it to the imprisoner
	
	    player.setBedSpawnLocation(null); // clear the players spawn location
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		if (pearlstorage.getByImprisoned(player) != null) {
			player.sendMessage("Your prison pearl has bound you to this bleak and endless world");
			event.setRespawnLocation(prisonlocation);
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
		
		if (event.getType() == PrisonPearlEvent.Type.HELD) {
			pp.getImprisonedPlayer().sendMessage(pp.getHolder().getDisplayName() + " now holds your prison pearl");
		} else if (event.getType() == PrisonPearlEvent.Type.STORED) {
			pp.getImprisonedPlayer().sendMessage("Your prison pearl is now stored at " + pp.getLocation());
		}
	}
}
