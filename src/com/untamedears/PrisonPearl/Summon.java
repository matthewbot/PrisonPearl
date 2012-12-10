package com.untamedears.PrisonPearl;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class Summon {
	private final String summonedname;
	private final Location returnloc;
	private int alloweddistance;
    private int damageamount;
    private boolean canSpeak;
    private boolean canDealDamage;
    private boolean canBreakBlocks;

    public Summon(String summonedname, Location returnloc, int alloweddistance, int damageamount, boolean canSpeak, boolean canDealDamage, boolean canBreakBlocks) {
		this.summonedname = summonedname;
		this.returnloc = returnloc;
		this.alloweddistance = alloweddistance;
        this.damageamount = damageamount;
        this.canSpeak = canSpeak;
        this.canDealDamage = canDealDamage;
        this.canBreakBlocks = canBreakBlocks;
    }
	
	public String getSummonedName() {
		return summonedname;
	}
	
	public Player getSummonedPlayer() {
		return Bukkit.getPlayerExact(summonedname);
	}
	
	public Location getReturnLocation() {
		return returnloc;
	}
	
	public int getAllowedDistance() {
		return alloweddistance;
	}
	
	public void setAllowedDistance(int alloweddistance) {
		this.alloweddistance = alloweddistance;
	}

    public int getDamageAmount() {
        return damageamount;
    }

    public void setDamageAmount(int damageamount) {
        this.damageamount = damageamount;
    }

    public boolean isCanSpeak() {
        return canSpeak;
    }

    public void setCanSpeak(boolean canSpeak) {
        this.canSpeak = canSpeak;
    }

    public boolean isCanDealDamage() {
        return canDealDamage;
    }

    public void setCanDealDamage(boolean canDealDamage) {
        this.canDealDamage = canDealDamage;
    }

    public boolean isCanBreakBlocks() {
        return canBreakBlocks;
    }

    public void setCanBreakBlocks(boolean canBreakBlocks) {
        this.canBreakBlocks = canBreakBlocks;
    }
}
