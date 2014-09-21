package net.minecraft.server;

public class Block18 extends Block {
	
	public static final String[] types_sponge = { "sponge", "wet_sponge" };
	public static final String[] types_prismarine = { "prismarine", "prismarine_bricks", "dark_prismarine" };
	
	protected Block18(Material material) {
		super(material);
		a(CreativeModeTab.b);
	}
	
	@Override
	public int getDropData(int paramInt) {
		return paramInt;
	}
}