package net.minecraft.server;

import org.github.paperspigot.PaperSpigotConfig;

public class MobEffectAttackDamage extends MobEffectList {

	protected MobEffectAttackDamage(int i, boolean flag, int j) {
		super(i, flag, j);
	}

	@Override
	public double a(int i, AttributeModifier attributemodifier) {
		// PaperSpigot - Configurable modifiers for strength and weakness effects
		return id == MobEffectList.WEAKNESS.id ? (double) (PaperSpigotConfig.weaknessEffectModifier * (i + 1)) : PaperSpigotConfig.strengthEffectModifier * (i + 1);
	}
}
