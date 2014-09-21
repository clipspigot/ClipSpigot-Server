package org.bukkit.craftbukkit;

import net.minecraft.server.BiomeBase;

import org.bukkit.ChunkSnapshot;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.block.CraftBlock;

/**
 * Represents a static, thread-safe snapshot of chunk of blocks
 * Purpose is to allow clean, efficient copy of a chunk data to be made, and then handed off for processing in another thread (e.g. map rendering)
 */
public class CraftChunkSnapshot implements ChunkSnapshot {
	private final int x, z;
	private final String worldname;
	private final short[][] blockids; /* Block IDs, by section */
	private final byte[][] blockdata;
	private final byte[][] skylight;
	private final byte[][] emitlight;
	private final boolean[] empty;
	private final int[] hmap; // Height map
	private final long captureFulltime;
	private final BiomeBase[] biome;
	private final double[] biomeTemp;
	private final double[] biomeRain;

	CraftChunkSnapshot(int x, int z, String wname, long wtime, short[][] sectionBlockIDs, byte[][] sectionBlockData, byte[][] sectionSkyLights, byte[][] sectionEmitLights, boolean[] sectionEmpty, int[] hmap, BiomeBase[] biome, double[] biomeTemp, double[] biomeRain) {
		this.x = x;
		this.z = z;
		worldname = wname;
		captureFulltime = wtime;
		blockids = sectionBlockIDs;
		blockdata = sectionBlockData;
		skylight = sectionSkyLights;
		emitlight = sectionEmitLights;
		empty = sectionEmpty;
		this.hmap = hmap;
		this.biome = biome;
		this.biomeTemp = biomeTemp;
		this.biomeRain = biomeRain;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getZ() {
		return z;
	}

	@Override
	public String getWorldName() {
		return worldname;
	}

	@Override
	public final int getBlockTypeId(int x, int y, int z) {
		return blockids[y >> 4][(y & 0xF) << 8 | z << 4 | x];
	}

	@Override
	public final int getBlockData(int x, int y, int z) {
		int off = (y & 0xF) << 7 | z << 3 | x >> 1;
		return blockdata[y >> 4][off] >> ((x & 1) << 2) & 0xF;
	}

	@Override
	public final int getBlockSkyLight(int x, int y, int z) {
		int off = (y & 0xF) << 7 | z << 3 | x >> 1;
		return skylight[y >> 4][off] >> ((x & 1) << 2) & 0xF;
	}

	@Override
	public final int getBlockEmittedLight(int x, int y, int z) {
		int off = (y & 0xF) << 7 | z << 3 | x >> 1;
		return emitlight[y >> 4][off] >> ((x & 1) << 2) & 0xF;
	}

	@Override
	public final int getHighestBlockYAt(int x, int z) {
		return hmap[z << 4 | x];
	}

	@Override
	public final Biome getBiome(int x, int z) {
		return CraftBlock.biomeBaseToBiome(biome[z << 4 | x]);
	}

	@Override
	public final double getRawBiomeTemperature(int x, int z) {
		return biomeTemp[z << 4 | x];
	}

	@Override
	public final double getRawBiomeRainfall(int x, int z) {
		return biomeRain[z << 4 | x];
	}

	@Override
	public final long getCaptureFullTime() {
		return captureFulltime;
	}

	@Override
	public final boolean isSectionEmpty(int sy) {
		return empty[sy];
	}
}
