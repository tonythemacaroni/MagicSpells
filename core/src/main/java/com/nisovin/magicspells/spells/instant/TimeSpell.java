package com.nisovin.magicspells.spells.instant;

import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class TimeSpell extends InstantSpell implements TargetedLocationSpell {

	private int timeToSet;

	private String strAnnounce;
		
	public TimeSpell(MagicConfig config, String spellName) {
		super(config, spellName);
		
		timeToSet = getConfigInt("time-to-set", 0);
		strAnnounce = getConfigString("str-announce", "The sun suddenly appears in the sky.");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			World world = caster.getWorld();
			setTime(world);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		setTime(target.getWorld());
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		setTime(target.getWorld());
		return true;
	}

	private void setTime(World world) {
		world.setTime(timeToSet);
		for (Player p : world.getPlayers()) sendMessage(strAnnounce, p, MagicSpells.NULL_ARGS);
	}

	public int getTimeToSet() {
		return timeToSet;
	}

	public void setTimeToSet(int timeToSet) {
		this.timeToSet = timeToSet;
	}

	public String getStrAnnounce() {
		return strAnnounce;
	}

	public void setStrAnnounce(String strAnnounce) {
		this.strAnnounce = strAnnounce;
	}

}
