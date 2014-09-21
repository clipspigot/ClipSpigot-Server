package net.minecraft.server;

import java.util.Random;

public class BlockStone extends Block {

	public static final String[] types = { "stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite" };
	
	public BlockStone() {
		super(Material.STONE);
		a(CreativeModeTab.b);
	}

	@Override
	public Item getDropType(int paramInt1, Random paramRandom, int paramInt2) {
		return Item.getItemOf(Blocks.COBBLESTONE);
	}

	@Override
	public int getDropData(int paramInt) {
		return paramInt;
	}
}