package com.nisovin.magicspells.spells.instant;

import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

import org.bukkit.util.Vector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;
import com.nisovin.magicspells.Subspell;
import com.nisovin.magicspells.util.Util;
import org.bukkit.event.entity.EntityDamageEvent;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class LeapSpell extends InstantSpell {

	private final Set<UUID> jumping;

	private final String landSpellName;

	private float rotation;
	private float upwardVelocity;
	private float forwardVelocity;

	private boolean clientOnly;
	private boolean cancelDamage;
	private boolean addVelocityInstead;

	private Subspell landSpell;

	public LeapSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		jumping = new HashSet<>();

		rotation = getConfigFloat("rotation", 0F);
		upwardVelocity = getConfigFloat("upward-velocity", 15F) / 10F;
		forwardVelocity = getConfigFloat("forward-velocity", 40F) / 10F;

		clientOnly = getConfigBoolean("client-only", false);
		cancelDamage = getConfigBoolean("cancel-damage", true);
		addVelocityInstead = getConfigBoolean("add-velocity-instead", false);

		landSpellName = getConfigString("land-spell", "");
	}

	@Override
	public void initialize() {
		super.initialize();

		landSpell = new Subspell(landSpellName);
		if (!landSpell.process()) {
			if (!landSpellName.isEmpty()) MagicSpells.error("LeapSpell '" + internalName + "' has an invalid land-spell defined!");
			landSpell = null;
		}
	}

	public boolean isJumping(LivingEntity livingEntity) {
		return jumping.contains(livingEntity.getUniqueId());
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Vector v = caster.getLocation().getDirection();
			v.setY(0).normalize().multiply(forwardVelocity * power).setY(upwardVelocity * power);
			if (rotation != 0) Util.rotateVector(v, rotation);
			v = Util.makeFinite(v);

			if (clientOnly && caster instanceof Player) MagicSpells.getVolatileCodeHandler().setClientVelocity((Player) caster, v);
			else {
				if (addVelocityInstead) caster.setVelocity(caster.getVelocity().add(v));
				else caster.setVelocity(v);
			}

			jumping.add(caster.getUniqueId());
			playSpellEffects(EffectPosition.CASTER, caster);
		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
		LivingEntity livingEntity = (LivingEntity) event.getEntity();
		if (!jumping.remove(livingEntity.getUniqueId())) return;
		if (landSpell != null) landSpell.cast(livingEntity, 1F);
		playSpellEffects(EffectPosition.TARGET, livingEntity.getLocation());
		if (cancelDamage) event.setCancelled(true);
	}

	public Set<UUID> getJumping() {
		return jumping;
	}

	public float getRotation() {
		return rotation;
	}

	public void setRotation(float rotation) {
		this.rotation = rotation;
	}

	public float getUpwardVelocity() {
		return upwardVelocity;
	}

	public void setUpwardVelocity(float upwardVelocity) {
		this.upwardVelocity = upwardVelocity;
	}

	public float getForwardVelocity() {
		return forwardVelocity;
	}

	public void setForwardVelocity(float forwardVelocity) {
		this.forwardVelocity = forwardVelocity;
	}

	public boolean isClientOnly() {
		return clientOnly;
	}

	public void setClientOnly(boolean clientOnly) {
		this.clientOnly = clientOnly;
	}

	public boolean shouldCancelDamage() {
		return cancelDamage;
	}

	public void setCancelDamage(boolean cancelDamage) {
		this.cancelDamage = cancelDamage;
	}

	public boolean shouldAddVelocityInstead() {
		return addVelocityInstead;
	}

	public void setAddVelocityInstead(boolean addVelocityInstead) {
		this.addVelocityInstead = addVelocityInstead;
	}

	public Subspell getLandSpell() {
		return landSpell;
	}

	public void setLandSpell(Subspell landSpell) {
		this.landSpell = landSpell;
	}

}
