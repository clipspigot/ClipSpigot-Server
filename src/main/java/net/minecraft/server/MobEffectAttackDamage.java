package net.minecraft.server;

import org.clipspigot.ClipSpigotConfig;

public class MobEffectAttackDamage extends MobEffectList {

	protected MobEffectAttackDamage(int i, boolean flag, int j) {
		super(i, flag, j);
	}

	@Override
	public double a(int i, AttributeModifier attributemodifier) {
		// PaperSpigot - Configurable modifiers for strength and weakness effects
		return id == MobEffectList.WEAKNESS.id ? (double) (ClipSpigotConfig.weaknessEffectModifier * (i + 1)) : ClipSpigotConfig.strengthEffectModifier * (i + 1);
	}
}
