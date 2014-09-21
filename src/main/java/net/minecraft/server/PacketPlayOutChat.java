package net.minecraft.server;

import java.io.IOException;

public class PacketPlayOutChat extends Packet {

    private IChatBaseComponent a;
    private boolean b;

    public PacketPlayOutChat() {
        this.b = true;
    }

    public PacketPlayOutChat(IChatBaseComponent ichatbasecomponent) {
        this(ichatbasecomponent, true);
    }

    public PacketPlayOutChat(IChatBaseComponent ichatbasecomponent, boolean flag) {
        this.b = true;
        this.a = ichatbasecomponent;
        this.b = flag;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException {
        this.a = ChatSerializer.a(packetdataserializer.c(32767));
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException {
        packetdataserializer.a(ChatSerializer.a(this.a));
        // Spigot start - protocol patch
        if ( packetdataserializer.version >= 16 )
        {
            packetdataserializer.writeByte(0);
        }
        // Spigot end
    }

    public void a(PacketPlayOutListener packetplayoutlistener) {
        packetplayoutlistener.a(this);
    }

    public String b() {
        return String.format("message=\'%s\'", new Object[] { this.a});
    }

    public boolean d() {
        return this.b;
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketPlayOutListener) packetlistener);
    }
}
