package com.nisovin.magicspells.spells.instant;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.player.PlayerMoveEvent;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.BoundingBox;
import com.nisovin.magicspells.util.MagicConfig;
import com.nisovin.magicspells.util.SpellReagents;
import com.nisovin.magicspells.spells.InstantSpell;
import com.nisovin.magicspells.events.SpellTargetEvent;
import com.nisovin.magicspells.spelleffects.EffectPosition;

public class PortalSpell extends InstantSpell {

	private final String firstMarkSpellName;
	private final String secondMarkSpellName;

	private MarkSpell firstMark;
	private MarkSpell secondMark;

	private SpellReagents teleportCost;

	private int duration;
	private int minDistanceSq;
	private int maxDistanceSq;
	private int effectInterval;
	private int teleportCooldown;

	private float vertRadius;
	private float horizRadius;

	private boolean allowReturn;
	private boolean tpOtherPlayers;
	private boolean usingSecondMarkSpell;
	private boolean chargeCostToTeleporter;

	private String strNoMark;
	private String strTooFar;
	private String strTooClose;
	private String strTeleportCostFail;
	private String strTeleportCooldownFail;

	public PortalSpell(MagicConfig config, String spellName) {
		super(config, spellName);

		firstMarkSpellName = getConfigString("mark-spell", "");
		secondMarkSpellName = getConfigString("second-mark-spell", "");

		teleportCost = getConfigReagents("teleport-cost");

		duration = getConfigInt("duration", 400);
		minDistanceSq = getConfigInt("min-distance", 10);
		maxDistanceSq = getConfigInt("max-distance", 0);
		effectInterval = getConfigInt("effect-interval", 10);
		teleportCooldown = getConfigInt("teleport-cooldown", 5) * 1000;

		horizRadius = getConfigFloat("horiz-radius", 1F);
		vertRadius = getConfigFloat("vert-radius", 1F);

		allowReturn = getConfigBoolean("allow-return", true);
		tpOtherPlayers = getConfigBoolean("teleport-other-players", true);
		chargeCostToTeleporter = getConfigBoolean("charge-cost-to-teleporter", false);

		strNoMark = getConfigString("str-no-mark", "You have not marked a location to make a portal to.");
		strTooFar = getConfigString("str-too-far", "You are too far away from your marked location.");
		strTooClose = getConfigString("str-too-close", "You are too close to your marked location.");
		strTeleportCostFail = getConfigString("str-teleport-cost-fail", "");
		strTeleportCooldownFail = getConfigString("str-teleport-cooldown-fail", "");

		minDistanceSq *= minDistanceSq;
		maxDistanceSq *= maxDistanceSq;
	}

	@Override
	public void initialize() {
		super.initialize();

		Spell spell = MagicSpells.getSpellByInternalName(firstMarkSpellName);
		if (spell instanceof MarkSpell) firstMark = (MarkSpell) spell;
		else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid mark-spell defined!");

		usingSecondMarkSpell = false;
		if (!secondMarkSpellName.isEmpty()) {
			spell = MagicSpells.getSpellByInternalName(secondMarkSpellName);
			if (spell instanceof MarkSpell) {
				secondMark = (MarkSpell) spell;
				usingSecondMarkSpell = true;
			} else MagicSpells.error("PortalSpell '" + internalName + "' has an invalid second-mark-spell defined!");
		}
	}

	@Override
	public PostCastAction castSpell(LivingEntity caster, SpellCastState state, float power, String[] args) {
		if (state == SpellCastState.NORMAL) {
			Location loc = firstMark.getEffectiveMark(caster);
			Location locSecond;
			if (loc == null) {
				sendMessage(strNoMark, caster, args);
				return PostCastAction.ALREADY_HANDLED;
			}

			if (usingSecondMarkSpell) {
				locSecond = secondMark.getEffectiveMark(caster);
				if (locSecond == null) {
					sendMessage(strNoMark, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				}
			} else locSecond = caster.getLocation();

			double distanceSq = 0;
			if (maxDistanceSq > 0) {
				if (!loc.getWorld().equals(locSecond.getWorld())) {
					sendMessage(strTooFar, caster, args);
					return PostCastAction.ALREADY_HANDLED;
				} else {
					distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq > maxDistanceSq) {
						sendMessage(strTooFar, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}
			if (minDistanceSq > 0) {
				if (loc.getWorld().equals(locSecond.getWorld())) {
					if (distanceSq == 0) distanceSq = locSecond.distanceSquared(loc);
					if (distanceSq < minDistanceSq) {
						sendMessage(strTooClose, caster, args);
						return PostCastAction.ALREADY_HANDLED;
					}
				}
			}

			new PortalLink(this, caster, loc, locSecond);
			playSpellEffects(EffectPosition.CASTER, caster);

		}
		return PostCastAction.HANDLE_NORMALLY;
	}

	public MarkSpell getFirstMark() {
		return firstMark;
	}

	public void setFirstMark(MarkSpell firstMark) {
		this.firstMark = firstMark;
	}

	public MarkSpell getSecondMark() {
		return secondMark;
	}

	public void setSecondMark(MarkSpell secondMark) {
		this.secondMark = secondMark;
	}

	public SpellReagents getTeleportCost() {
		return teleportCost;
	}

	public void setTeleportCost(SpellReagents teleportCost) {
		this.teleportCost = teleportCost;
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration(int duration) {
		this.duration = duration;
	}

	public int getMinDistanceSq() {
		return minDistanceSq;
	}

	public void setMinDistanceSq(int minDistanceSq) {
		this.minDistanceSq = minDistanceSq;
	}

	public int getMaxDistanceSq() {
		return maxDistanceSq;
	}

	public void setMaxDistanceSq(int maxDistanceSq) {
		this.maxDistanceSq = maxDistanceSq;
	}

	public int getEffectInterval() {
		return effectInterval;
	}

	public void setEffectInterval(int effectInterval) {
		this.effectInterval = effectInterval;
	}

	public int getTeleportCooldown() {
		return teleportCooldown;
	}

	public void setTeleportCooldown(int teleportCooldown) {
		this.teleportCooldown = teleportCooldown;
	}

	public float getVertRadius() {
		return vertRadius;
	}

	public void setVertRadius(float vertRadius) {
		this.vertRadius = vertRadius;
	}

	public float getHorizRadius() {
		return horizRadius;
	}

	public void setHorizRadius(float horizRadius) {
		this.horizRadius = horizRadius;
	}

	public boolean shouldAllowReturn() {
		return allowReturn;
	}

	public void setAllowReturn(boolean allowReturn) {
		this.allowReturn = allowReturn;
	}

	public boolean shouldTpOtherPlayers() {
		return tpOtherPlayers;
	}

	public void setTpOtherPlayers(boolean tpOtherPlayers) {
		this.tpOtherPlayers = tpOtherPlayers;
	}

	public boolean isUsingSecondMarkSpell() {
		return usingSecondMarkSpell;
	}

	public void setUsingSecondMarkSpell(boolean usingSecondMarkSpell) {
		this.usingSecondMarkSpell = usingSecondMarkSpell;
	}

	public boolean shouldChargeCostToTeleporter() {
		return chargeCostToTeleporter;
	}

	public void setChargeCostToTeleporter(boolean chargeCostToTeleporter) {
		this.chargeCostToTeleporter = chargeCostToTeleporter;
	}

	public String getStrNoMark() {
		return strNoMark;
	}

	public void setStrNoMark(String strNoMark) {
		this.strNoMark = strNoMark;
	}

	public String getStrTooFar() {
		return strTooFar;
	}

	public void setStrTooFar(String strTooFar) {
		this.strTooFar = strTooFar;
	}

	public String getStrTooClose() {
		return strTooClose;
	}

	public void setStrTooClose(String strTooClose) {
		this.strTooClose = strTooClose;
	}

	public String getStrTeleportCostFail() {
		return strTeleportCostFail;
	}

	public void setStrTeleportCostFail(String strTeleportCostFail) {
		this.strTeleportCostFail = strTeleportCostFail;
	}

	public String getStrTeleportCooldownFail() {
		return strTeleportCooldownFail;
	}

	public void setStrTeleportCooldownFail(String strTeleportCooldownFail) {
		this.strTeleportCooldownFail = strTeleportCooldownFail;
	}

	private class PortalLink implements Listener {

		private PortalSpell spell;
		private LivingEntity caster;
		private Location loc1;
		private Location loc2;
		private BoundingBox box1;
		private BoundingBox box2;
		private int taskId1 = -1;
		private int taskId2 = -1;
		private Map<String, Long> cooldownUntil;

		private PortalLink (PortalSpell spell, LivingEntity caster, Location loc1, Location loc2) {
			this.spell = spell;
			this.caster = caster;
			this.loc1 = loc1;
			this.loc2 = loc2;

			box1 = new BoundingBox(loc1, spell.horizRadius, spell.vertRadius);
			box2 = new BoundingBox(loc2, spell.horizRadius, spell.vertRadius);
			cooldownUntil = new HashMap<>();

			cooldownUntil.put(caster.getName(), System.currentTimeMillis() + spell.teleportCooldown);
			registerEvents(this);
			startTasks();
		}

		private void startTasks() {
			if (spell.effectInterval > 0) {
				taskId1 = MagicSpells.scheduleRepeatingTask(() -> {
					if (caster.isValid()) {
						playSpellEffects(EffectPosition.SPECIAL, loc1);
						playSpellEffects(EffectPosition.SPECIAL, loc2);
					} else disable();

				}, spell.effectInterval, spell.effectInterval);
			}
			taskId2 = MagicSpells.scheduleDelayedTask(this::disable, spell.duration);
		}

		@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
		private void onMove(PlayerMoveEvent event) {
			if (!tpOtherPlayers && !event.getPlayer().equals(caster)) return;
			if (!caster.isValid()) {
				disable();
				return;
			}
			Player player = event.getPlayer();
			if (box1.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc2.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playSpellEffects(EffectPosition.TARGET, player);
				}
			} else if (spell.allowReturn && box2.contains(event.getTo())) {
				if (checkTeleport(player)) {
					Location loc = loc1.clone();
					loc.setYaw(player.getLocation().getYaw());
					loc.setPitch(player.getLocation().getPitch());
					event.setTo(loc);
					playSpellEffects(EffectPosition.TARGET, player);
				}
			}
		}

		private boolean checkTeleport(LivingEntity livingEntity) {
			if (cooldownUntil.containsKey(livingEntity.getName()) && cooldownUntil.get(livingEntity.getName()) > System.currentTimeMillis()) {
				sendMessage(strTeleportCooldownFail, livingEntity, MagicSpells.NULL_ARGS);
				return false;
			}
			cooldownUntil.put(livingEntity.getName(), System.currentTimeMillis() + teleportCooldown);

			LivingEntity payer = null;
			if (spell.teleportCost != null) {
				if (spell.chargeCostToTeleporter) {
					if (hasReagents(livingEntity, spell.teleportCost)) {
						payer = livingEntity;
					} else {
						sendMessage(spell.strTeleportCostFail, livingEntity, MagicSpells.NULL_ARGS);
						return false;
					}
				} else {
					if (hasReagents(caster, spell.teleportCost)) {
						payer = caster;
					} else {
						sendMessage(spell.strTeleportCostFail, livingEntity, MagicSpells.NULL_ARGS);
						return false;
					}
				}
				if (payer == null) return false;
			}

			SpellTargetEvent event = new SpellTargetEvent(spell, caster, livingEntity, 1);
			Bukkit.getPluginManager().callEvent(event);
			if (payer != null) removeReagents(payer, spell.teleportCost);
			return true;
		}

		private void disable() {
			unregisterEvents(this);
			playSpellEffects(EffectPosition.DELAYED, loc1);
			playSpellEffects(EffectPosition.DELAYED, loc2);
			if (taskId1 > 0) MagicSpells.cancelTask(taskId1);
			if (taskId2 > 0) MagicSpells.cancelTask(taskId2);
			caster = null;
			spell = null;
			loc1 = null;
			loc2 = null;
			box1 = null;
			box2 = null;
			cooldownUntil.clear();
			cooldownUntil = null;
		}

	}

}
