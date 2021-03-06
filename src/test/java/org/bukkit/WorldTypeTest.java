package org.bukkit;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import net.minecraft.server.WorldType;

import org.junit.Test;

public class WorldTypeTest {
	@Test
	public void testTypes() {
		for (WorldType type : WorldType.types) {
			if (type == null) {
				continue;
			}

			assertThat(type.name() + " has no Bukkit world", org.bukkit.WorldType.getByName(type.name()), is(not(nullValue())));
		}
	}
}
