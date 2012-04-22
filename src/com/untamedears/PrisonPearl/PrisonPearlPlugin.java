package com.untamedears.PrisonPearl;

import java.lang.reflect.Method;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class PrisonPearlPlugin extends JavaPlugin implements Listener {
	private PearlTagList taglist;
	private PrisonPearlStorage pearlstorage;
	
	public void onEnable() {
		taglist = new PearlTagList(this, 20*10);
		pearlstorage = new PrisonPearlStorage();
		
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
		if (!(event.getEntity() instanceof Player))
			return;
		
		Player player = (Player)event.getEntity();
		taglist.taggerExpired(player);
		if (taglist.taggedImprisoned(player)) {
			System.out.println("Imprisoned " + player.getName());
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
			tagger.sendMessage("Your tag for " + tagged.getDisplayName() + " was switched to " + other.getDisplayName());
		}
	}
}
