package com.untamedears.PrisonPearl;

import java.util.logging.Logger;

import org.bukkit.Server;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.trc202.CombatTag.CombatTag;
import com.trc202.CombatTagApi.CombatTagApi;

@SuppressWarnings("ALL")
class CombatTagManager {
	private CombatTagApi combatTagApi;
	private boolean combatTagEnabled = false;

	public CombatTagManager(Server server, Logger l) {
		if(server.getPluginManager().getPlugin("CombatTag") != null) {
			combatTagApi = new CombatTagApi((CombatTag)server.getPluginManager().getPlugin("CombatTag"));
			combatTagEnabled = true;
		}
	}
	
	public boolean isCombatTagNPC(Entity player) {
        return combatTagEnabled && combatTagApi != null && combatTagApi.isNPC(player);
    }
	
	public boolean isCombatTaged(Player player) {
        return combatTagEnabled && combatTagApi != null && combatTagApi.isInCombat(player);
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
