package com.nisovin.magicspells.spells.passive;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.event.EventHandler;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.Spell;
import com.nisovin.magicspells.util.SpellFilter;
import com.nisovin.magicspells.Spell.SpellCastState;
import com.nisovin.magicspells.events.SpellCastEvent;
import com.nisovin.magicspells.util.OverridePriority;
import com.nisovin.magicspells.spells.passive.util.PassiveListener;

// Optional trigger variable of comma separated list of internal spell names to accept
public class SpellCastListener extends PassiveListener {

	private SpellFilter filter;

	@Override
	public void initialize(String var) {
		if (var == null || var.isEmpty()) return;

		List<String> spells = new ArrayList<>();
		List<String> deniedSpells = new ArrayList<>();
		List<String> tagList = new ArrayList<>();
		List<String> deniedTagList = new ArrayList<>();

		String[] split = var.split(",");
		for (String s : split) {
			boolean denied = false;
			s = s.trim();

			if (s.startsWith("!")) {
				s = s.substring(1);
				denied = true;
			}

			if (s.toLowerCase().startsWith("tag:")) {
				if (denied) {
					deniedTagList.add(s.substring(4));
				} else {
					tagList.add(s.substring(4));
				}
			} else {
				if (denied) {
					deniedSpells.add(s);
				} else {
					spells.add(s);
				}
			}
		}

		filter = new SpellFilter(spells, deniedSpells, tagList, deniedTagList);
	}

	@OverridePriority
	@EventHandler
	public void onSpellCast(SpellCastEvent event) {
		if (event.getSpellCastState() != SpellCastState.NORMAL) return;
		if (!isCancelStateOk(event.isCancelled())) return;

		LivingEntity caster = event.getCaster();
		if (!hasSpell(caster) || !canTrigger(caster)) return;

		Spell spell = event.getSpell();
		if (spell.equals(passiveSpell)) return;
		if (filter != null && !filter.check(spell)) return;

		boolean casted = passiveSpell.activate(caster);
		if (cancelDefaultAction(casted)) event.setCancelled(true);
	}

}
