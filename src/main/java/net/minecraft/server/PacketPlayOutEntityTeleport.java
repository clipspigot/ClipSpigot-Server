package net.minecraft.server;

public class PacketPlayOutEntityTeleport extends Packet {

	private int a;
	private int b;
	private int c;
	private int d;
	private byte e;
	private byte f;
	private boolean onGround; // Spigot - protocol patch

	public PacketPlayOutEntityTeleport() {
	}

	public PacketPlayOutEntityTeleport(Entity entity) {
		a = entity.getId();
		b = MathHelper.floor(entity.locX * 32.0D);
		c = MathHelper.floor(entity.locY * 32.0D);
		d = MathHelper.floor(entity.locZ * 32.0D);
		e = (byte) (int) (entity.yaw * 256.0F / 360.0F);
		f = (byte) (int) (entity.pitch * 256.0F / 360.0F);
	}

	public PacketPlayOutEntityTeleport(int i, int j, int k, int l, byte b0, byte b1, boolean onGround) { // Spigot - protocol patch
		a = i;
		b = j;
		c = k;
		d = l;
		e = b0;
		f = b1;
		this.onGround = onGround; // Spigot - protocol patch
	}

	@Override
	public void a(PacketDataSerializer packetdataserializer) {
		a = packetdataserializer.readInt();
		b = packetdataserializer.readInt();
		c = packetdataserializer.readInt();
		d = packetdataserializer.readInt();
		e = packetdataserializer.readByte();
		f = packetdataserializer.readByte();
	}

	@Override
	public void b(PacketDataSerializer packetdataserializer) {
		// Spigot start - protocol
		if (packetdataserializer.version < 16) {
			packetdataserializer.writeInt(a);
		} else {
			packetdataserializer.b(a);
		}
		// Spigot end
		packetdataserializer.writeInt(b);
		packetdataserializer.writeInt(c);
		packetdataserializer.writeInt(d);
		packetdataserializer.writeByte(e);
		packetdataserializer.writeByte(f);
		// Spigot start - protocol patch
		if (packetdataserializer.version >= 22) {
			packetdataserializer.writeBoolean(onGround);
		}
		// Spigot end
	}

	public void a(PacketPlayOutListener packetplayoutlistener) {
		packetplayoutlistener.a(this);
	}

	@Override
	public void handle(PacketListener packetlistener) {
		this.a((PacketPlayOutListener) packetlistener);
	}
}
