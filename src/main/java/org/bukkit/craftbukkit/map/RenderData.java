package org.bukkit.craftbukkit.map;

import java.util.ArrayList;

import org.bukkit.map.MapCursor;

public class RenderData {

	public final byte[] buffer;
	public final ArrayList<MapCursor> cursors;

	public RenderData() {
		buffer = new byte[128 * 128];
		cursors = new ArrayList<MapCursor>();
	}

}
