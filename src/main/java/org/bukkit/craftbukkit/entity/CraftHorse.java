package org.bukkit.craftbukkit.entity;

import java.util.UUID;

import net.minecraft.server.EntityHorse;

import org.apache.commons.lang.Validate;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.inventory.CraftInventoryHorse;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.inventory.HorseInventory;

public class CraftHorse extends CraftAnimals implements Horse {

	public CraftHorse(CraftServer server, EntityHorse entity) {
		super(server, entity);
	}

	@Override
	public EntityHorse getHandle() {
		return (EntityHorse) entity;
	}

	@Override
	public Variant getVariant() {
		return Variant.values()[getHandle().getType()];
	}

	@Override
	public void setVariant(Variant variant) {
		Validate.notNull(variant, "Variant cannot be null");
		getHandle().setType(variant.ordinal());
	}

	@Override
	public Color getColor() {
		return Color.values()[getHandle().getVariant() & 0xFF];
	}

	@Override
	public void setColor(Color color) {
		Validate.notNull(color, "Color cannot be null");
		getHandle().setVariant(color.ordinal() & 0xFF | getStyle().ordinal() << 8);
	}

	@Override
	public Style getStyle() {
		return Style.values()[getHandle().getVariant() >>> 8];
	}

	@Override
	public void setStyle(Style style) {
		Validate.notNull(style, "Style cannot be null");
		getHandle().setVariant(getColor().ordinal() & 0xFF | style.ordinal() << 8);
	}

	@Override
	public boolean isCarryingChest() {
		return getHandle().hasChest();
	}

	@Override
	public void setCarryingChest(boolean chest) {
		if (chest == isCarryingChest())
			return;
		getHandle().setHasChest(chest);
		getHandle().loadChest();
	}

	@Override
	public int getDomestication() {
		return getHandle().getTemper();
	}

	@Override
	public void setDomestication(int value) {
		Validate.isTrue(value >= 0, "Domestication cannot be less than zero");
		Validate.isTrue(value <= getMaxDomestication(), "Domestication cannot be greater than the max domestication");
		getHandle().setTemper(value);
	}

	@Override
	public int getMaxDomestication() {
		return getHandle().getMaxDomestication();
	}

	@Override
	public void setMaxDomestication(int value) {
		Validate.isTrue(value > 0, "Max domestication cannot be zero or less");
		getHandle().maxDomestication = value;
	}

	@Override
	public double getJumpStrength() {
		return getHandle().getJumpStrength();
	}

	@Override
	public void setJumpStrength(double strength) {
		Validate.isTrue(strength >= 0, "Jump strength cannot be less than zero");
		getHandle().getAttributeInstance(EntityHorse.attributeJumpStrength).setValue(strength);
	}

	@Override
	public boolean isTamed() {
		return getHandle().isTame();
	}

	@Override
	public void setTamed(boolean tamed) {
		getHandle().setTame(tamed);
	}

	@Override
	public AnimalTamer getOwner() {
		if (getOwnerUUID() == null)
			return null;
		return getServer().getOfflinePlayer(getOwnerUUID());
	}

	@Override
	public void setOwner(AnimalTamer owner) {
		if (owner != null) {
			setTamed(true);
			getHandle().setPathEntity(null);
			setOwnerUUID(owner.getUniqueId());
		} else {
			setTamed(false);
			setOwnerUUID(null);
		}
	}

	public UUID getOwnerUUID() {
		try {
			return UUID.fromString(getHandle().getOwnerUUID());
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	public void setOwnerUUID(UUID uuid) {
		if (uuid == null) {
			getHandle().setOwnerUUID("");
		} else {
			getHandle().setOwnerUUID(uuid.toString());
		}
	}

	@Override
	public HorseInventory getInventory() {
		return new CraftInventoryHorse(getHandle().inventoryChest);
	}

	@Override
	public String toString() {
		return "CraftHorse{variant=" + getVariant() + ", owner=" + getOwner() + '}';
	}

	@Override
	public EntityType getType() {
		return EntityType.HORSE;
	}
}
