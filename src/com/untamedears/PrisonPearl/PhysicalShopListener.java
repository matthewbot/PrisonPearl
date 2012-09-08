package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;

import com.wolvereness.physicalshop.Shop;
import com.wolvereness.physicalshop.ShopMaterial;
import com.wolvereness.physicalshop.events.ShopInteractEvent;

class PhysicalShopListener implements Listener {
	@SuppressWarnings("WeakerAccess")
    final
    PrisonPearlStorage pearls;
	
	public PhysicalShopListener(PrisonPearlPlugin plugin, PrisonPearlStorage pearls) {
		this.pearls = pearls;
		
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void onShopInteract(ShopInteractEvent event) {
		if (event.getAction() != Action.LEFT_CLICK_BLOCK)
			return;
		
		Shop shop = event.getShop();
		if (shop.getShopItems() == 0)
			return;
		
		ShopMaterial mat = shop.getMaterial();
		if (mat == null || mat.getMaterial() != Material.ENDER_PEARL || mat.getDurability() == 0)
			return;
		
		PrisonPearl pp = pearls.getByID(mat.getDurability());
		if (pp == null)
			return;
		
		event.getPlayer().sendMessage("The pearl sold by the shop contains " + pp.getImprisonedName());
	}
}
