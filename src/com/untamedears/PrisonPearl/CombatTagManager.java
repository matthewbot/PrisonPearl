package com.untamedears.PrisonPearl;

import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.trc202.CombatTag.CombatTag;
import com.trc202.CombatTagApi.CombatTagApi;

public class CombatTagManager {
	private CombatTagApi combatTagApi;
	private boolean combatTagEnabled = false;

	public CombatTagManager(Server server, Logger l) {
        Logger log = l;
		if(server.getPluginManager().getPlugin("CombatTag") != null) {
			combatTagApi = new CombatTagApi((CombatTag)server.getPluginManager().getPlugin("CombatTag"));
			combatTagEnabled = true;
		}
	}
	
	public boolean isCombatTagNPC(Entity player) {
		if (combatTagEnabled && combatTagApi != null) {
			return combatTagApi.isNPC(player);
		}
		return false;
	}
	
	public boolean isCombatTaged(Player player) {
		if (combatTagEnabled && combatTagApi != null) {
			return combatTagApi.isInCombat(player);
		}
		return false;
	}
	
	public String getNPCPlayerName(Entity player) {
		if (combatTagEnabled && combatTagApi != null) {
			if (combatTagApi.isNPC(player)) {
				return combatTagApi.getNPCPlayerName(player);
			}
		}
		return "";
	}
}
