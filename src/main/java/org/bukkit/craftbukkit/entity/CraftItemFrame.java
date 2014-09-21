package org.bukkit.craftbukkit.entity;

import net.minecraft.server.EntityItemFrame;
import net.minecraft.server.WorldServer;

import org.apache.commons.lang.Validate;
import org.bukkit.Rotation;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;

public class CraftItemFrame extends CraftHanging implements ItemFrame {
	public CraftItemFrame(CraftServer server, EntityItemFrame entity) {
		super(server, entity);
	}

	@Override
	public boolean setFacingDirection(BlockFace face, boolean force) {
		if (!super.setFacingDirection(face, force))
			return false;

		WorldServer world = ((CraftWorld) getWorld()).getHandle();
		world.getTracker().untrackEntity(getHandle());
		world.getTracker().track(getHandle());
		return true;
	}

	@Override
	public void setItem(org.bukkit.inventory.ItemStack item) {
		if (item == null || item.getTypeId() == 0) {
			getHandle().getDataWatcher().add(2, 5);
			getHandle().getDataWatcher().update(2);
		} else {
			getHandle().setItem(CraftItemStack.asNMSCopy(item));
		}
	}

	@Override
	public org.bukkit.inventory.ItemStack getItem() {
		return CraftItemStack.asBukkitCopy(getHandle().getItem());
	}

	@Override
	public Rotation getRotation() {
		return toBukkitRotation(getHandle().getRotation());
	}

	Rotation toBukkitRotation(int value) {
		// Translate NMS rotation integer to Bukkit API
		switch (value) {
		case 0:
			return Rotation.NONE;
		case 1:
			return Rotation.CLOCKWISE;
		case 2:
			return Rotation.FLIPPED;
		case 3:
			return Rotation.COUNTER_CLOCKWISE;
		default:
			throw new AssertionError("Unknown rotation " + value + " for " + getHandle());
		}
	}

	@Override
	public void setRotation(Rotation rotation) {
		Validate.notNull(rotation, "Rotation cannot be null");
		getHandle().setRotation(toInteger(rotation));
	}

	static int toInteger(Rotation rotation) {
		// Translate Bukkit API rotation to NMS integer
		switch (rotation) {
		case NONE:
			return 0;
		case CLOCKWISE:
			return 1;
		case FLIPPED:
			return 2;
		case COUNTER_CLOCKWISE:
			return 3;
		default:
			throw new IllegalArgumentException(rotation + " is not applicable to an ItemFrame");
		}
	}

	@Override
	public EntityItemFrame getHandle() {
		return (EntityItemFrame) entity;
	}

	@Override
	public String toString() {
		return "CraftItemFrame{item=" + getItem() + ", rotation=" + getRotation() + "}";
	}

	@Override
	public EntityType getType() {
		return EntityType.ITEM_FRAME;
	}
}
