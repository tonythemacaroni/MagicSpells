package com.nisovin.magicspells.spells.targeted;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TargetInfo;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.TargetedSpell;
import com.nisovin.magicspells.spells.TargetedEntitySpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;
import com.nisovin.magicspells.spells.TargetedLocationSpell;

public class RotateSpell extends TargetedSpell implements TargetedEntitySpell, TargetedLocationSpell {

	private int rotationYaw;
	private int rotationPitch;

	private boolean random;
	private boolean affectPitch;
	private boolean mimicDirection;

	private String face;

	public RotateSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		rotationYaw = getConfigInt("rotation-yaw", 10);
		rotationPitch = getConfigInt("rotation-pitch", 0);

		random = getConfigBoolean("random", false);
		affectPitch = getConfigBoolean("affect-pitch", false);
		mimicDirection = getConfigBoolean("mimic-direction", false);

		face = getConfigString("face", "");
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			TargetInfo<LivingEntity> target = getTargetedEntity(caster, power);
			if (target == null) return noTarget(caster);
			spin(caster, target.getTarget());
			playSpellEffects(caster, target.getTarget());
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@Override
	public boolean castAtEntity(LivingEntity caster, LivingEntity target, float power) {
		playSpellEffects(caster, target);
		spin(caster, target);
		return true;
	}

	@Override
	public boolean castAtEntity(LivingEntity target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		spin(target);
		return true;
	}

	@Override
	public boolean castAtLocation(LivingEntity caster, Location target, float power) {
		playSpellEffects(EffectPosition.TARGET, target);
		spin(caster, target);
		return true;
	}

	@Override
	public boolean castAtLocation(Location target, float power) {
		return false;
	}

	private void spin(LivingEntity target) {
		Location loc = target.getLocation();
		if (random) {
			loc.setYaw(Util.getRandomInt(360));
			if (affectPitch) loc.setPitch(Util.getRandomInt(181) - 90);
		} else {
			loc.setYaw(loc.getYaw() + rotationYaw);
			if (affectPitch) loc.setPitch(loc.getPitch() + rotationPitch);
		}
		target.teleport(loc);
	}

	private void spin(LivingEntity caster, LivingEntity target) {
		Location targetLoc = target.getLocation();
		Location casterLoc = caster.getLocation();

		if (face.isEmpty()) {
			spin(target);
			return;
		}

		Location loc;
		switch (face) {
			case "target" -> caster.teleport(changeDirection(casterLoc, targetLoc));
			case "caster" -> target.teleport(changeDirection(targetLoc, casterLoc));
			case "away-from-caster" -> {
				loc = changeDirection(targetLoc, casterLoc);
				loc.setYaw(loc.getYaw() + 180);
				target.teleport(loc);
			}
			case "away-from-target" -> {
				loc = changeDirection(casterLoc, targetLoc);
				loc.setYaw(loc.getYaw() + 180);
				caster.teleport(loc);
			}
		}

	}

	private void spin(LivingEntity entity, Location target) {
		entity.teleport(changeDirection(entity.getLocation(), target));
	}

	private Location changeDirection(Location pos1, Location pos2) {
		Location loc = pos1.clone();
		if (mimicDirection) {
			if (affectPitch) loc.setPitch(pos2.getPitch());
			loc.setYaw(pos2.getYaw());
		} else loc.setDirection(getVectorDir(pos1, pos2));

		return loc;
	}

	private Vector getVectorDir(Location caster, Location target) {
		return target.clone().subtract(caster.toVector()).toVector();
	}

}
