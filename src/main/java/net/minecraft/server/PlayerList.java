package net.minecraft.server;

import java.io.File;
import java.net.SocketAddress;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.util.com.google.common.base.Charsets;
import net.minecraft.util.com.google.common.collect.Lists;
import net.minecraft.util.com.google.common.collect.Maps;
import net.minecraft.util.com.mojang.authlib.GameProfile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.TravelAgent;
// CraftBukkit start
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.chunkio.ChunkIOExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

// CraftBukkit end

public abstract class PlayerList {

	public static final File a = new File("banned-players.json");
	public static final File b = new File("banned-ips.json");
	public static final File c = new File("ops.json");
	public static final File d = new File("whitelist.json");
	private static final Logger g = LogManager.getLogger();
	private static final SimpleDateFormat h = new SimpleDateFormat("yyyy-MM-dd \'at\' HH:mm:ss z");
	private final MinecraftServer server;
	public final List players = new java.util.concurrent.CopyOnWriteArrayList(); // CraftBukkit - ArrayList -> CopyOnWriteArrayList: Iterator safety
	private final GameProfileBanList j;
	private final IpBanList k;
	private final OpList operators;
	private final WhiteList whitelist;
	private final Map n;
	public IPlayerFileData playerFileData; // CraftBukkit - private -> public
	public boolean hasWhitelist; // CraftBukkit - private -> public
	protected int maxPlayers;
	private int q;
	private EnumGamemode r;
	private boolean s;
	private int t;

	// CraftBukkit start
	private CraftServer cserver;

	public PlayerList(MinecraftServer minecraftserver) {
		minecraftserver.server = new CraftServer(minecraftserver, this);
		minecraftserver.console = org.bukkit.craftbukkit.command.ColouredConsoleSender.getInstance();
		minecraftserver.reader.addCompleter(new org.bukkit.craftbukkit.command.ConsoleCommandCompleter(minecraftserver.server));
		cserver = minecraftserver.server;
		// CraftBukkit end

		j = new GameProfileBanList(a);
		k = new IpBanList(b);
		operators = new OpList(c);
		whitelist = new WhiteList(d);
		n = Maps.newHashMap();
		server = minecraftserver;
		j.a(false);
		k.a(false);
		maxPlayers = 8;
	}

	public void a(NetworkManager networkmanager, EntityPlayer entityplayer) {
		GameProfile gameprofile = entityplayer.getProfile();
		UserCache usercache = server.getUserCache();
		GameProfile gameprofile1 = usercache.a(gameprofile.getId());
		String s = gameprofile1 == null ? gameprofile.getName() : gameprofile1.getName();

		usercache.a(gameprofile);
		NBTTagCompound nbttagcompound = this.a(entityplayer);

		entityplayer.spawnIn(server.getWorldServer(entityplayer.dimension));
		entityplayer.playerInteractManager.a((WorldServer) entityplayer.world);
		String s1 = "local";

		if (networkmanager.getSocketAddress() != null) {
			s1 = networkmanager.getSocketAddress().toString();
		}

		// Spigot start - spawn location event
		Player bukkitPlayer = entityplayer.getBukkitEntity();
		PlayerSpawnLocationEvent ev = new PlayerSpawnLocationEvent(bukkitPlayer, bukkitPlayer.getLocation());
		Bukkit.getPluginManager().callEvent(ev);

		Location loc = ev.getSpawnLocation();
		WorldServer world = ((CraftWorld) loc.getWorld()).getHandle();

		entityplayer.spawnIn(world);
		entityplayer.setPosition(loc.getX(), loc.getY(), loc.getZ());
		entityplayer.b(loc.getYaw(), loc.getPitch()); // should be setYawAndPitch
		// Spigot end

		// CraftBukkit - Moved message to after join
		// g.info(entityplayer.getName() + "[" + s1 + "] logged in with entity id " + entityplayer.getId() + " at (" + entityplayer.locX + ", " + entityplayer.locY + ", " + entityplayer.locZ + ")");
		WorldServer worldserver = server.getWorldServer(entityplayer.dimension);
		ChunkCoordinates chunkcoordinates = worldserver.getSpawn();

		this.a(entityplayer, (EntityPlayer) null, worldserver);
		PlayerConnection playerconnection = new PlayerConnection(server, networkmanager, entityplayer);

		// CraftBukkit start - Don't send a higher than 60 MaxPlayer size, otherwise the PlayerInfo window won't render correctly.
		int maxPlayers = getMaxPlayers();
		if (maxPlayers > 60) {
			maxPlayers = 60;
		}
		playerconnection.sendPacket(new PacketPlayOutLogin(entityplayer.getId(), entityplayer.playerInteractManager.getGameMode(), worldserver.getWorldData().isHardcore(), worldserver.worldProvider.dimension, worldserver.difficulty, maxPlayers, worldserver.getWorldData().getType()));
		entityplayer.getBukkitEntity().sendSupportedChannels();
		// CraftBukkit end
		playerconnection.sendPacket(new PacketPlayOutCustomPayload("MC|Brand", getServer().getServerModName().getBytes(Charsets.UTF_8)));
		playerconnection.sendPacket(new PacketPlayOutSpawnPosition(chunkcoordinates.x, chunkcoordinates.y, chunkcoordinates.z));
		playerconnection.sendPacket(new PacketPlayOutAbilities(entityplayer.abilities));
		playerconnection.sendPacket(new PacketPlayOutHeldItemSlot(entityplayer.inventory.itemInHandIndex));
		entityplayer.getStatisticManager().d();
		entityplayer.getStatisticManager().updateStatistics(entityplayer);
		sendScoreboard((ScoreboardServer) worldserver.getScoreboard(), entityplayer);
		server.az();
		/* CraftBukkit start - login message is handled in the event
		ChatMessage chatmessage;

		if (!entityplayer.getName().equalsIgnoreCase(s)) {
		    chatmessage = new ChatMessage("multiplayer.player.joined.renamed", new Object[] { entityplayer.getScoreboardDisplayName(), s});
		} else {
		    chatmessage = new ChatMessage("multiplayer.player.joined", new Object[] { entityplayer.getScoreboardDisplayName()});
		}

		chatmessage.getChatModifier().setColor(EnumChatFormat.YELLOW);
		this.sendMessage(chatmessage);
		// CraftBukkit end */
		c(entityplayer);
		worldserver = server.getWorldServer(entityplayer.dimension); // CraftBukkit - Update in case join event changed it
		playerconnection.a(entityplayer.locX, entityplayer.locY, entityplayer.locZ, entityplayer.yaw, entityplayer.pitch);
		this.b(entityplayer, worldserver);
		if (server.getResourcePack().length() > 0) {
			entityplayer.setResourcePack(server.getResourcePack());
		}

		Iterator iterator = entityplayer.getEffects().iterator();

		while (iterator.hasNext()) {
			MobEffect mobeffect = (MobEffect) iterator.next();

			playerconnection.sendPacket(new PacketPlayOutEntityEffect(entityplayer.getId(), mobeffect));
		}

		entityplayer.syncInventory();
		if (nbttagcompound != null && nbttagcompound.hasKeyOfType("Riding", 10)) {
			Entity entity = EntityTypes.a(nbttagcompound.getCompound("Riding"), worldserver);

			if (entity != null) {
				entity.attachedToPlayer = true;
				worldserver.addEntity(entity);
				entityplayer.mount(entity);
				entity.attachedToPlayer = false;
			}
		}

		// CraftBukkit - Moved from above, added world
		g.info(entityplayer.getName() + "[" + s1 + "] logged in with entity id " + entityplayer.getId() + " at ([" + entityplayer.world.worldData.getName() + "] " + entityplayer.locX + ", " + entityplayer.locY + ", " + entityplayer.locZ + ")");
	}

	public void sendScoreboard(ScoreboardServer scoreboardserver, EntityPlayer entityplayer) { // CraftBukkit - protected -> public
		HashSet hashset = new HashSet();
		Iterator iterator = scoreboardserver.getTeams().iterator();

		while (iterator.hasNext()) {
			ScoreboardTeam scoreboardteam = (ScoreboardTeam) iterator.next();

			entityplayer.playerConnection.sendPacket(new PacketPlayOutScoreboardTeam(scoreboardteam, 0));
		}

		for (int i = 0; i < 3; ++i) {
			ScoreboardObjective scoreboardobjective = scoreboardserver.getObjectiveForSlot(i);

			if (scoreboardobjective != null && !hashset.contains(scoreboardobjective)) {
				List list = scoreboardserver.getScoreboardScorePacketsForObjective(scoreboardobjective);
				Iterator iterator1 = list.iterator();

				while (iterator1.hasNext()) {
					Packet packet = (Packet) iterator1.next();

					entityplayer.playerConnection.sendPacket(packet);
				}

				hashset.add(scoreboardobjective);
			}
		}
	}

	public void setPlayerFileData(WorldServer[] aworldserver) {
		if (playerFileData != null)
			return; // CraftBukkit
		playerFileData = aworldserver[0].getDataManager().getPlayerFileData();
	}

	public void a(EntityPlayer entityplayer, WorldServer worldserver) {
		WorldServer worldserver1 = entityplayer.r();

		if (worldserver != null) {
			worldserver.getPlayerChunkMap().removePlayer(entityplayer);
		}

		worldserver1.getPlayerChunkMap().addPlayer(entityplayer);
		worldserver1.chunkProviderServer.getChunkAt((int) entityplayer.locX >> 4, (int) entityplayer.locZ >> 4);
	}

	public int d() {
		return PlayerChunkMap.getFurthestViewableBlock(s());
	}

	public NBTTagCompound a(EntityPlayer entityplayer) {
		// CraftBukkit - fix reference to worldserver array
		NBTTagCompound nbttagcompound = server.worlds.get(0).getWorldData().i();
		NBTTagCompound nbttagcompound1;

		if (entityplayer.getName().equals(server.M()) && nbttagcompound != null) {
			entityplayer.f(nbttagcompound);
			nbttagcompound1 = nbttagcompound;
			g.debug("loading single player");
		} else {
			nbttagcompound1 = playerFileData.load(entityplayer);
		}

		return nbttagcompound1;
	}

	protected void b(EntityPlayer entityplayer) {
		playerFileData.save(entityplayer);
		ServerStatisticManager serverstatisticmanager = (ServerStatisticManager) n.get(entityplayer.getUniqueID());

		if (serverstatisticmanager != null) {
			serverstatisticmanager.b();
		}
	}

	public void c(EntityPlayer entityplayer) {
		cserver.detectListNameConflict(entityplayer); // CraftBukkit
		// this.sendAll(new PacketPlayOutPlayerInfo(entityplayer.getName(), true, 1000)); // CraftBukkit - replaced with loop below
		players.add(entityplayer);
		WorldServer worldserver = server.getWorldServer(entityplayer.dimension);

		// CraftBukkit start
		PlayerJoinEvent playerJoinEvent = new PlayerJoinEvent(cserver.getPlayer(entityplayer), "\u00A7e" + entityplayer.getName() + " joined the game.");
		cserver.getPluginManager().callEvent(playerJoinEvent);

		String joinMessage = playerJoinEvent.getJoinMessage();

		if (joinMessage != null && joinMessage.length() > 0) {
			for (IChatBaseComponent line : org.bukkit.craftbukkit.util.CraftChatMessage.fromString(joinMessage)) {
				server.getPlayerList().sendAll(new PacketPlayOutChat(line));
			}
		}
		cserver.onPlayerJoin(playerJoinEvent.getPlayer());

		ChunkIOExecutor.adjustPoolSize(getPlayerCount());
		// CraftBukkit end

		// CraftBukkit start - Only add if the player wasn't moved in the event
		if (entityplayer.world == worldserver && !worldserver.players.contains(entityplayer)) {
			worldserver.addEntity(entityplayer);
			this.a(entityplayer, (WorldServer) null);
		}
		// CraftBukkit end

		// CraftBukkit start - sendAll above replaced with this loop
		PacketPlayOutPlayerInfo packet = PacketPlayOutPlayerInfo.addPlayer(entityplayer); // Spigot - protocol patch
		for (int i = 0; i < players.size(); ++i) {
			EntityPlayer entityplayer1 = (EntityPlayer) players.get(i);

			if (entityplayer1.getBukkitEntity().canSee(entityplayer.getBukkitEntity())) {
				entityplayer1.playerConnection.sendPacket(packet);
			}
		}
		// CraftBukkit end

		for (int i = 0; i < players.size(); ++i) {
			EntityPlayer entityplayer1 = (EntityPlayer) players.get(i);

			// CraftBukkit start
			if (!entityplayer.getBukkitEntity().canSee(entityplayer1.getBukkitEntity())) {
				continue;
			}
			// .name -> .listName
			entityplayer.playerConnection.sendPacket(PacketPlayOutPlayerInfo.addPlayer(entityplayer1)); // Spigot - protocol patch
			// CraftBukkit end
		}
	}

	public void d(EntityPlayer entityplayer) {
		entityplayer.r().getPlayerChunkMap().movePlayer(entityplayer);
	}

	public String disconnect(EntityPlayer entityplayer) { // CraftBukkit - return string
		entityplayer.a(StatisticList.f);

		// CraftBukkit start - Quitting must be before we do final save of data, in case plugins need to modify it
		org.bukkit.craftbukkit.event.CraftEventFactory.handleInventoryCloseEvent(entityplayer);

		PlayerQuitEvent playerQuitEvent = new PlayerQuitEvent(cserver.getPlayer(entityplayer), "\u00A7e" + entityplayer.getName() + " left the game.");
		cserver.getPluginManager().callEvent(playerQuitEvent);
		entityplayer.getBukkitEntity().disconnect(playerQuitEvent.getQuitMessage());
		// CraftBukkit end

		this.b(entityplayer);
		WorldServer worldserver = entityplayer.r();

		if (entityplayer.vehicle != null && !(entityplayer.vehicle instanceof EntityPlayer)) { // CraftBukkit - Don't remove players
			worldserver.removeEntity(entityplayer.vehicle);
			g.debug("removing player mount");
		}

		worldserver.kill(entityplayer);
		worldserver.getPlayerChunkMap().removePlayer(entityplayer);
		players.remove(entityplayer);
		n.remove(entityplayer.getUniqueID());
		ChunkIOExecutor.adjustPoolSize(getPlayerCount()); // CraftBukkit

		// CraftBukkit start - .name -> .listName, replace sendAll with loop
		// this.sendAll(new PacketPlayOutPlayerInfo(entityplayer.getName(), false, 9999));
		PacketPlayOutPlayerInfo packet = PacketPlayOutPlayerInfo.removePlayer(entityplayer); // Spigot - protocol patch
		for (int i = 0; i < players.size(); ++i) {
			EntityPlayer entityplayer1 = (EntityPlayer) players.get(i);

			if (entityplayer1.getBukkitEntity().canSee(entityplayer.getBukkitEntity())) {
				entityplayer1.playerConnection.sendPacket(packet);
			} else {
				entityplayer1.getBukkitEntity().removeDisconnectingPlayer(entityplayer.getBukkitEntity());
			}
		}
		// This removes the scoreboard (and player reference) for the specific player in the manager
		cserver.getScoreboardManager().removePlayer(entityplayer.getBukkitEntity());

		return playerQuitEvent.getQuitMessage();
		// CraftBukkit end
	}

	// CraftBukkit start - Whole method, SocketAddress to LoginListener, added hostname to signature, return EntityPlayer
	public EntityPlayer attemptLogin(LoginListener loginlistener, GameProfile gameprofile, String hostname) {
		// Instead of kicking then returning, we need to store the kick reason
		// in the event, check with plugins to see if it's ok, and THEN kick
		// depending on the outcome.
		SocketAddress socketaddress = loginlistener.networkManager.getSocketAddress();

		EntityPlayer entity = new EntityPlayer(server, server.getWorldServer(0), gameprofile, new PlayerInteractManager(server.getWorldServer(0)));
		Player player = entity.getBukkitEntity();
		PlayerLoginEvent event = new PlayerLoginEvent(player, hostname, ((java.net.InetSocketAddress) socketaddress).getAddress(), ((java.net.InetSocketAddress) loginlistener.networkManager.getRawAddress()).getAddress());
		String s;

		if (j.isBanned(gameprofile) && !j.get(gameprofile).hasExpired()) {
			GameProfileBanEntry gameprofilebanentry = (GameProfileBanEntry) j.get(gameprofile);

			s = "You are banned from this server!\nReason: " + gameprofilebanentry.getReason();
			if (gameprofilebanentry.getExpires() != null) {
				s = s + "\nYour ban will be removed on " + h.format(gameprofilebanentry.getExpires());
			}

			// return s;
			if (!gameprofilebanentry.hasExpired()) {
				event.disallow(PlayerLoginEvent.Result.KICK_BANNED, s); // Spigot
			}
		} else if (!isWhitelisted(gameprofile)) {
			// return "You are not white-listed on this server!";
			event.disallow(PlayerLoginEvent.Result.KICK_WHITELIST, org.spigotmc.SpigotConfig.whitelistMessage); // Spigot
		} else if (k.isBanned(socketaddress) && !k.get(socketaddress).hasExpired()) { // Spigot
			IpBanEntry ipbanentry = k.get(socketaddress);

			s = "Your IP address is banned from this server!\nReason: " + ipbanentry.getReason();
			if (ipbanentry.getExpires() != null) {
				s = s + "\nYour ban will be removed on " + h.format(ipbanentry.getExpires());
			}

			// return s;
			event.disallow(PlayerLoginEvent.Result.KICK_BANNED, s);
		} else {
			// return this.players.size() >= this.maxPlayers ? "The server is full!" : null;
			if (players.size() >= maxPlayers) {
				event.disallow(PlayerLoginEvent.Result.KICK_FULL, org.spigotmc.SpigotConfig.serverFullMessage); // Spigot
			}
		}

		cserver.getPluginManager().callEvent(event);
		if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
			loginlistener.disconnect(event.getKickMessage());
			return null;
		}

		return entity;
		// CraftBukkit end
	}

	public EntityPlayer processLogin(GameProfile gameprofile, EntityPlayer player) { // CraftBukkit - added EntityPlayer
		UUID uuid = EntityHuman.a(gameprofile);
		ArrayList arraylist = Lists.newArrayList();

		EntityPlayer entityplayer;

		for (int i = 0; i < players.size(); ++i) {
			entityplayer = (EntityPlayer) players.get(i);
			if (entityplayer.getUniqueID().equals(uuid)) {
				arraylist.add(entityplayer);
			}
		}

		Iterator iterator = arraylist.iterator();

		while (iterator.hasNext()) {
			entityplayer = (EntityPlayer) iterator.next();
			entityplayer.playerConnection.disconnect("You logged in from another location");
		}

		/* CraftBukkit start
		Object object;

		if (this.server.R()) {
		    object = new DemoPlayerInteractManager(this.server.getWorldServer(0));
		} else {
		    object = new PlayerInteractManager(this.server.getWorldServer(0));
		}

		return new EntityPlayer(this.server, this.server.getWorldServer(0), gameprofile, (PlayerInteractManager) object);
		// */
		return player;
		// CraftBukkit end
	}

	// CraftBukkit start
	public EntityPlayer moveToWorld(EntityPlayer entityplayer, int i, boolean flag) {
		return this.moveToWorld(entityplayer, i, flag, null, true);
	}

	public EntityPlayer moveToWorld(EntityPlayer entityplayer, int i, boolean flag, Location location, boolean avoidSuffocation) {
		// CraftBukkit end
		entityplayer.r().getTracker().untrackPlayer(entityplayer);
		// entityplayer.r().getTracker().untrackEntity(entityplayer); // CraftBukkit
		entityplayer.r().getPlayerChunkMap().removePlayer(entityplayer);
		// this.players.remove(entityplayer); // PaperSpigot -- Fixes BUKKIT-4561 and BUKKIT-4082 and BUKKIT-2094
		server.getWorldServer(entityplayer.dimension).removeEntity(entityplayer);
		ChunkCoordinates chunkcoordinates = entityplayer.getBed();
		boolean flag1 = entityplayer.isRespawnForced();

		/* CraftBukkit start
		entityplayer.dimension = i;
		Object object;

		if (this.server.R()) {
		    object = new DemoPlayerInteractManager(this.server.getWorldServer(entityplayer.dimension));
		} else {
		    object = new PlayerInteractManager(this.server.getWorldServer(entityplayer.dimension));
		}

		EntityPlayer entityplayer1 = new EntityPlayer(this.server, this.server.getWorldServer(entityplayer.dimension), entityplayer.getProfile(), (PlayerInteractManager) object);
		// */
		EntityPlayer entityplayer1 = entityplayer;
		org.bukkit.World fromWorld = entityplayer1.getBukkitEntity().getWorld();
		entityplayer1.viewingCredits = false;
		// CraftBukkit end

		entityplayer1.playerConnection = entityplayer.playerConnection;
		entityplayer1.copyTo(entityplayer, flag);
		entityplayer1.d(entityplayer.getId());
		// WorldServer worldserver = this.server.getWorldServer(entityplayer.dimension); // CraftBukkit - handled later

		// this.a(entityplayer1, entityplayer, worldserver); // CraftBukkit - removed
		ChunkCoordinates chunkcoordinates1;

		// CraftBukkit start - fire PlayerRespawnEvent
		if (location == null) {
			boolean isBedSpawn = false;
			CraftWorld cworld = (CraftWorld) server.server.getWorld(entityplayer.spawnWorld);
			if (cworld != null && chunkcoordinates != null) {
				chunkcoordinates1 = EntityHuman.getBed(cworld.getHandle(), chunkcoordinates, flag1);
				if (chunkcoordinates1 != null) {
					isBedSpawn = true;
					location = new Location(cworld, chunkcoordinates1.x + 0.5, chunkcoordinates1.y, chunkcoordinates1.z + 0.5);
				} else {
					entityplayer1.setRespawnPosition(null, true);
					entityplayer1.playerConnection.sendPacket(new PacketPlayOutGameStateChange(0, 0));
				}
			}

			if (location == null) {
				cworld = (CraftWorld) server.server.getWorlds().get(0);
				chunkcoordinates = cworld.getHandle().getSpawn();
				location = new Location(cworld, chunkcoordinates.x + 0.5, chunkcoordinates.y, chunkcoordinates.z + 0.5);
			}

			Player respawnPlayer = cserver.getPlayer(entityplayer1);
			PlayerRespawnEvent respawnEvent = new PlayerRespawnEvent(respawnPlayer, location, isBedSpawn);
			cserver.getPluginManager().callEvent(respawnEvent);
			// Spigot Start
			if (entityplayer.playerConnection.isDisconnected())
				return entityplayer;

			location = respawnEvent.getRespawnLocation();
			entityplayer.reset();
		} else {
			location.setWorld(server.getWorldServer(i).getWorld());
		}
		WorldServer worldserver = ((CraftWorld) location.getWorld()).getHandle();
		entityplayer1.setLocation(location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
		// CraftBukkit end

		worldserver.chunkProviderServer.getChunkAt((int) entityplayer1.locX >> 4, (int) entityplayer1.locZ >> 4);

		while (avoidSuffocation && !worldserver.getCubes(entityplayer1, entityplayer1.boundingBox).isEmpty()) { // CraftBukkit
			entityplayer1.setPosition(entityplayer1.locX, entityplayer1.locY + 1.0D, entityplayer1.locZ);
		}

		// CraftBukkit start
		byte actualDimension = (byte) worldserver.getWorld().getEnvironment().getId();
		// Force the client to refresh their chunk cache.
		entityplayer1.playerConnection.sendPacket(new PacketPlayOutRespawn((byte) (actualDimension >= 0 ? -1 : 0), worldserver.difficulty, worldserver.getWorldData().getType(), entityplayer.playerInteractManager.getGameMode()));
		entityplayer1.playerConnection.sendPacket(new PacketPlayOutRespawn(actualDimension, worldserver.difficulty, worldserver.getWorldData().getType(), entityplayer1.playerInteractManager.getGameMode()));
		entityplayer1.spawnIn(worldserver);
		entityplayer1.dead = false;
		entityplayer1.playerConnection.teleport(new Location(worldserver.getWorld(), entityplayer1.locX, entityplayer1.locY, entityplayer1.locZ, entityplayer1.yaw, entityplayer1.pitch));
		entityplayer1.setSneaking(false);
		chunkcoordinates1 = worldserver.getSpawn();
		// entityplayer1.playerConnection.a(entityplayer1.locX, entityplayer1.locY, entityplayer1.locZ, entityplayer1.yaw, entityplayer1.pitch);
		// CraftBukkit end
		entityplayer1.playerConnection.sendPacket(new PacketPlayOutSpawnPosition(chunkcoordinates1.x, chunkcoordinates1.y, chunkcoordinates1.z));
		entityplayer1.playerConnection.sendPacket(new PacketPlayOutExperience(entityplayer1.exp, entityplayer1.expTotal, entityplayer1.expLevel));
		this.b(entityplayer1, worldserver);
		// CraftBukkit start
		// Don't re-add player to player list if disconnected
		if (!entityplayer.playerConnection.isDisconnected()) {
			worldserver.getPlayerChunkMap().addPlayer(entityplayer1);
			worldserver.addEntity(entityplayer1);
			// this.players.add(entityplayer1); // PaperSpigot -- Fixes BUKKIT-4561 and BUKKIT-4082 and BUKKIT-2094
		}
		// Added from changeDimension
		updateClient(entityplayer1); // Update health, etc...
		entityplayer1.updateAbilities();
		Iterator iterator = entityplayer1.getEffects().iterator();

		while (iterator.hasNext()) {
			MobEffect mobeffect = (MobEffect) iterator.next();

			entityplayer1.playerConnection.sendPacket(new PacketPlayOutEntityEffect(entityplayer1.getId(), mobeffect));
		}
		// entityplayer1.syncInventory();
		// CraftBukkit end
		entityplayer1.setHealth(entityplayer1.getHealth());

		// CraftBukkit start
		// Don't fire on respawn
		if (fromWorld != location.getWorld()) {
			PlayerChangedWorldEvent event = new PlayerChangedWorldEvent(entityplayer1.getBukkitEntity(), fromWorld);
			Bukkit.getServer().getPluginManager().callEvent(event);
		}

		// Save player file again if they were disconnected
		if (entityplayer.playerConnection.isDisconnected()) {
			this.b(entityplayer1);
		}
		// CraftBukkit end

		return entityplayer1;
	}

	// CraftBukkit start - Replaced the standard handling of portals with a more customised method.
	public void changeDimension(EntityPlayer entityplayer, int i, TeleportCause cause) {
		WorldServer exitWorld = null;
		if (entityplayer.dimension < CraftWorld.CUSTOM_DIMENSION_OFFSET) { // plugins must specify exit from custom Bukkit worlds
			// only target existing worlds (compensate for allow-nether/allow-end as false)
			for (WorldServer world : server.worlds) {
				if (world.dimension == i) {
					exitWorld = world;
				}
			}
		}

		Location enter = entityplayer.getBukkitEntity().getLocation();
		Location exit = null;
		boolean useTravelAgent = false; // don't use agent for custom worlds or return from THE_END
		if (exitWorld != null) {
			if (cause == TeleportCause.END_PORTAL && i == 0) {
				// THE_END -> NORMAL; use bed if available, otherwise default spawn
				exit = entityplayer.getBukkitEntity().getBedSpawnLocation();
				if (exit == null || ((CraftWorld) exit.getWorld()).getHandle().dimension != 0) {
					exit = exitWorld.getWorld().getSpawnLocation();
				}
			} else {
				// NORMAL <-> NETHER or NORMAL -> THE_END
				exit = calculateTarget(enter, exitWorld);
				useTravelAgent = true;
			}
		}

		TravelAgent agent = exit != null ? (TravelAgent) ((CraftWorld) exit.getWorld()).getHandle().getTravelAgent() : org.bukkit.craftbukkit.CraftTravelAgent.DEFAULT; // return arbitrary TA to compensate for implementation dependent plugins
		PlayerPortalEvent event = new PlayerPortalEvent(entityplayer.getBukkitEntity(), enter, exit, agent, cause);
		event.useTravelAgent(useTravelAgent);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled() || event.getTo() == null)
			return;

		exit = event.useTravelAgent() ? event.getPortalTravelAgent().findOrCreate(event.getTo()) : event.getTo();
		if (exit == null)
			return;
		exitWorld = ((CraftWorld) exit.getWorld()).getHandle();

		Vector velocity = entityplayer.getBukkitEntity().getVelocity();
		boolean before = exitWorld.chunkProviderServer.forceChunkLoad;
		exitWorld.chunkProviderServer.forceChunkLoad = true;
		exitWorld.getTravelAgent().adjustExit(entityplayer, exit, velocity);
		exitWorld.chunkProviderServer.forceChunkLoad = before;

		this.moveToWorld(entityplayer, exitWorld.dimension, true, exit, false); // Vanilla doesn't check for suffocation when handling portals, so neither should we
		if (entityplayer.motX != velocity.getX() || entityplayer.motY != velocity.getY() || entityplayer.motZ != velocity.getZ()) {
			entityplayer.getBukkitEntity().setVelocity(velocity);
		}
		// CraftBukkit end
	}

	public void a(Entity entity, int i, WorldServer worldserver, WorldServer worldserver1) {
		// CraftBukkit start - Split into modular functions
		Location exit = calculateTarget(entity.getBukkitEntity().getLocation(), worldserver1);
		repositionEntity(entity, exit, true);
	}

	// Copy of original a(Entity, int, WorldServer, WorldServer) method with only location calculation logic
	public Location calculateTarget(Location enter, World target) {
		WorldServer worldserver = ((CraftWorld) enter.getWorld()).getHandle();
		WorldServer worldserver1 = target.getWorld().getHandle();
		int i = worldserver.dimension;

		double y = enter.getY();
		float yaw = enter.getYaw();
		float pitch = enter.getPitch();
		double d0 = enter.getX();
		double d1 = enter.getZ();
		double d2 = 8.0D;
		/*
		double d3 = entity.locX;
		double d4 = entity.locY;
		double d5 = entity.locZ;
		float f = entity.yaw;

		worldserver.methodProfiler.a("moving");
		*/
		if (worldserver1.dimension == -1) {
			d0 /= d2;
			d1 /= d2;
			/*
			entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
			if (entity.isAlive()) {
			    worldserver.entityJoinedWorld(entity, false);
			}
			*/
		} else if (worldserver1.dimension == 0) {
			d0 *= d2;
			d1 *= d2;
			/*
			entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
			if (entity.isAlive()) {
			    worldserver.entityJoinedWorld(entity, false);
			}
			*/
		} else {
			ChunkCoordinates chunkcoordinates;

			if (i == 1) {
				// use default NORMAL world spawn instead of target
				worldserver1 = server.worlds.get(0);
				chunkcoordinates = worldserver1.getSpawn();
			} else {
				chunkcoordinates = worldserver1.getDimensionSpawn();
			}

			d0 = chunkcoordinates.x;
			y = chunkcoordinates.y;
			d1 = chunkcoordinates.z;
			yaw = 90.0F;
			pitch = 0.0F;
			/*
			entity.setPositionRotation(d0, entity.locY, d1, 90.0F, 0.0F);
			if (entity.isAlive()) {
			    worldserver.entityJoinedWorld(entity, false);
			}
			*/
		}

		// worldserver.methodProfiler.b();
		if (i != 1) {
			// worldserver.methodProfiler.a("placing");
			d0 = MathHelper.a((int) d0, -29999872, 29999872);
			d1 = MathHelper.a((int) d1, -29999872, 29999872);
			/*
			if (entity.isAlive()) {
			    worldserver1.addEntity(entity);
			    entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
			    worldserver1.entityJoinedWorld(entity, false);
			    worldserver1.getTravelAgent().a(entity, d3, d4, d5, f);
			}

			worldserver.methodProfiler.b();
			*/
		}

		// entity.spawnIn(worldserver1);
		return new Location(worldserver1.getWorld(), d0, y, d1, yaw, pitch);
	}

	// copy of original a(Entity, int, WorldServer, WorldServer) method with only entity repositioning logic
	public void repositionEntity(Entity entity, Location exit, boolean portal) {
		int i = entity.dimension;
		WorldServer worldserver = (WorldServer) entity.world;
		WorldServer worldserver1 = ((CraftWorld) exit.getWorld()).getHandle();
		/*
		double d0 = entity.locX;
		double d1 = entity.locZ;
		double d2 = 8.0D;
		double d3 = entity.locX;
		double d4 = entity.locY;
		double d5 = entity.locZ;
		float f = entity.yaw;
		*/

		worldserver.methodProfiler.a("moving");
		entity.setPositionRotation(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());
		if (entity.isAlive()) {
			worldserver.entityJoinedWorld(entity, false);
		}
		/*
		if (entity.dimension == -1) {
		    d0 /= d2;
		    d1 /= d2;
		    entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
		    if (entity.isAlive()) {
		        worldserver.entityJoinedWorld(entity, false);
		    }
		} else if (entity.dimension == 0) {
		    d0 *= d2;
		    d1 *= d2;
		    entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch);
		    if (entity.isAlive()) {
		        worldserver.entityJoinedWorld(entity, false);
		    }
		} else {
		    ChunkCoordinates chunkcoordinates;

		    if (i == 1) {
		        chunkcoordinates = worldserver1.getSpawn();
		    } else {
		        chunkcoordinates = worldserver1.getDimensionSpawn();
		    }

		    d0 = (double) chunkcoordinates.x;
		    entity.locY = (double) chunkcoordinates.y;
		    d1 = (double) chunkcoordinates.z;
		    entity.setPositionRotation(d0, entity.locY, d1, 90.0F, 0.0F);
		    if (entity.isAlive()) {
		        worldserver.entityJoinedWorld(entity, false);
		    }
		}
		*/

		worldserver.methodProfiler.b();
		if (i != 1) {
			worldserver.methodProfiler.a("placing");
			/*
			d0 = (double) MathHelper.a((int) d0, -29999872, 29999872);
			d1 = (double) MathHelper.a((int) d1, -29999872, 29999872);
			*/
			if (entity.isAlive()) {
				// entity.setPositionRotation(d0, entity.locY, d1, entity.yaw, entity.pitch)
				// worldserver1.getTravelAgent().a(entity, d3, d4, d5, f);
				if (portal) {
					Vector velocity = entity.getBukkitEntity().getVelocity();
					worldserver1.getTravelAgent().adjustExit(entity, exit, velocity);
					entity.setPositionRotation(exit.getX(), exit.getY(), exit.getZ(), exit.getYaw(), exit.getPitch());
					if (entity.motX != velocity.getX() || entity.motY != velocity.getY() || entity.motZ != velocity.getZ()) {
						entity.getBukkitEntity().setVelocity(velocity);
					}
				}
				worldserver1.addEntity(entity);
				worldserver1.entityJoinedWorld(entity, false);
			}

			worldserver.methodProfiler.b();
		}

		entity.spawnIn(worldserver1);
		// CraftBukkit end
	}

	private int currentPing = 0;

	public void tick() {
		if (++t > 600) {
			t = 0;
		}

		/* CraftBukkit start - Remove updating of lag to players -- it spams way to much on big servers.
		if (this.t < this.players.size()) {
		    EntityPlayer entityplayer = (EntityPlayer) this.players.get(this.p);

		    this.sendAll(new PacketPlayOutPlayerInfo(entityplayer.getName(), true, entityplayer.ping));
		}
		// CraftBukkit end */
		// Spigot start
		try {
			if (!players.isEmpty()) {
				currentPing = (currentPing + 1) % players.size();
				EntityPlayer player = (EntityPlayer) players.get(currentPing);
				if (player.lastPing == -1 || Math.abs(player.ping - player.lastPing) > 20) {
					Packet packet = PacketPlayOutPlayerInfo.updatePing(player); // Spigot - protocol patch
					for (EntityPlayer splayer : (List<EntityPlayer>) players) {
						if (splayer.getBukkitEntity().canSee(player.getBukkitEntity())) {
							splayer.playerConnection.sendPacket(packet);
						}
					}
					player.lastPing = player.ping;
				}
			}
		} catch (Exception e) {
			// Better safe than sorry :)
		}
		// Spigot end
	}

	public void sendAll(Packet packet) {
		for (int i = 0; i < players.size(); ++i) {
			((EntityPlayer) players.get(i)).playerConnection.sendPacket(packet);
		}
	}

	public void a(Packet packet, int i) {
		for (int j = 0; j < players.size(); ++j) {
			EntityPlayer entityplayer = (EntityPlayer) players.get(j);

			if (entityplayer.dimension == i) {
				entityplayer.playerConnection.sendPacket(packet);
			}
		}
	}

	public String b(boolean flag) {
		String s = "";
		ArrayList arraylist = Lists.newArrayList(players);

		for (int i = 0; i < arraylist.size(); ++i) {
			if (i > 0) {
				s = s + ", ";
			}

			s = s + ((EntityPlayer) arraylist.get(i)).getName();
			if (flag) {
				s = s + " (" + ((EntityPlayer) arraylist.get(i)).getUniqueID().toString() + ")";
			}
		}

		return s;
	}

	public String[] f() {
		String[] astring = new String[players.size()];

		for (int i = 0; i < players.size(); ++i) {
			astring[i] = ((EntityPlayer) players.get(i)).getName();
		}

		return astring;
	}

	public GameProfile[] g() {
		GameProfile[] agameprofile = new GameProfile[players.size()];

		for (int i = 0; i < players.size(); ++i) {
			agameprofile[i] = ((EntityPlayer) players.get(i)).getProfile();
		}

		return agameprofile;
	}

	public GameProfileBanList getProfileBans() {
		return j;
	}

	public IpBanList getIPBans() {
		return k;
	}

	public void addOp(GameProfile gameprofile) {
		operators.add(new OpListEntry(gameprofile, server.l()));

		// CraftBukkit start
		Player player = server.server.getPlayer(gameprofile.getId());
		if (player != null) {
			player.recalculatePermissions();
		}
		// CraftBukkit end
	}

	public void removeOp(GameProfile gameprofile) {
		operators.remove(gameprofile);

		// CraftBukkit start
		Player player = server.server.getPlayer(gameprofile.getId());
		if (player != null) {
			player.recalculatePermissions();
		}
		// CraftBukkit end
	}

	public boolean isWhitelisted(GameProfile gameprofile) {
		return !hasWhitelist || operators.d(gameprofile) || whitelist.d(gameprofile);
	}

	public boolean isOp(GameProfile gameprofile) {
		// CraftBukkit - fix reference to worldserver array
		return operators.d(gameprofile) || server.N() && server.worlds.get(0).getWorldData().allowCommands() && server.M().equalsIgnoreCase(gameprofile.getName()) || s;
	}

	public EntityPlayer getPlayer(String s) {
		Iterator iterator = players.iterator();

		EntityPlayer entityplayer;

		do {
			if (!iterator.hasNext())
				return null;

			entityplayer = (EntityPlayer) iterator.next();
		} while (!entityplayer.getName().equalsIgnoreCase(s));

		return entityplayer;
	}

	public List a(ChunkCoordinates chunkcoordinates, int i, int j, int k, int l, int i1, int j1, Map map, String s, String s1, World world) {
		if (players.isEmpty())
			return Collections.emptyList();
		else {
			Object object = new ArrayList();
			boolean flag = k < 0;
			boolean flag1 = s != null && s.startsWith("!");
			boolean flag2 = s1 != null && s1.startsWith("!");
			int k1 = i * i;
			int l1 = j * j;

			k = MathHelper.a(k);
			if (flag1) {
				s = s.substring(1);
			}

			if (flag2) {
				s1 = s1.substring(1);
			}

			for (int i2 = 0; i2 < players.size(); ++i2) {
				EntityPlayer entityplayer = (EntityPlayer) players.get(i2);

				if ((world == null || entityplayer.world == world) && (s == null || flag1 != s.equalsIgnoreCase(entityplayer.getName()))) {
					if (s1 != null) {
						ScoreboardTeamBase scoreboardteambase = entityplayer.getScoreboardTeam();
						String s2 = scoreboardteambase == null ? "" : scoreboardteambase.getName();

						if (flag2 == s1.equalsIgnoreCase(s2)) {
							continue;
						}
					}

					if (chunkcoordinates != null && (i > 0 || j > 0)) {
						float f = chunkcoordinates.e(entityplayer.getChunkCoordinates());

						if (i > 0 && f < k1 || j > 0 && f > l1) {
							continue;
						}
					}

					if (this.a(entityplayer, map) && (l == EnumGamemode.NONE.getId() || l == entityplayer.playerInteractManager.getGameMode().getId()) && (i1 <= 0 || entityplayer.expLevel >= i1) && entityplayer.expLevel <= j1) {
						((List) object).add(entityplayer);
					}
				}
			}

			if (chunkcoordinates != null) {
				Collections.sort((List) object, new PlayerDistanceComparator(chunkcoordinates));
			}

			if (flag) {
				Collections.reverse((List) object);
			}

			if (k > 0) {
				object = ((List) object).subList(0, Math.min(k, ((List) object).size()));
			}

			return (List) object;
		}
	}

	private boolean a(EntityHuman entityhuman, Map map) {
		if (map != null && map.size() != 0) {
			Iterator iterator = map.entrySet().iterator();

			Entry entry;
			boolean flag;
			int i;

			do {
				if (!iterator.hasNext())
					return true;

				entry = (Entry) iterator.next();
				String s = (String) entry.getKey();

				flag = false;
				if (s.endsWith("_min") && s.length() > 4) {
					flag = true;
					s = s.substring(0, s.length() - 4);
				}

				Scoreboard scoreboard = entityhuman.getScoreboard();
				ScoreboardObjective scoreboardobjective = scoreboard.getObjective(s);

				if (scoreboardobjective == null)
					return false;

				ScoreboardScore scoreboardscore = entityhuman.getScoreboard().getPlayerScoreForObjective(entityhuman.getName(), scoreboardobjective);

				i = scoreboardscore.getScore();
				if (i < ((Integer) entry.getValue()).intValue() && flag)
					return false;
			} while (i <= ((Integer) entry.getValue()).intValue() || flag);

			return false;
		} else
			return true;
	}

	public void sendPacketNearby(double d0, double d1, double d2, double d3, int i, Packet packet) {
		this.sendPacketNearby((EntityHuman) null, d0, d1, d2, d3, i, packet);
	}

	public void sendPacketNearby(EntityHuman entityhuman, double d0, double d1, double d2, double d3, int i, Packet packet) {
		for (int j = 0; j < players.size(); ++j) {
			EntityPlayer entityplayer = (EntityPlayer) players.get(j);

			// CraftBukkit start - Test if player receiving packet can see the source of the packet
			if (entityhuman != null && entityhuman instanceof EntityPlayer && !entityplayer.getBukkitEntity().canSee(((EntityPlayer) entityhuman).getBukkitEntity())) {
				continue;
			}
			// CraftBukkit end

			if (entityplayer != entityhuman && entityplayer.dimension == i) {
				double d4 = d0 - entityplayer.locX;
				double d5 = d1 - entityplayer.locY;
				double d6 = d2 - entityplayer.locZ;

				if (d4 * d4 + d5 * d5 + d6 * d6 < d3 * d3) {
					entityplayer.playerConnection.sendPacket(packet);
				}
			}
		}
	}

	public void savePlayers() {
		for (int i = 0; i < players.size(); ++i) {
			this.b((EntityPlayer) players.get(i));
		}
	}

	public void addWhitelist(GameProfile gameprofile) {
		whitelist.add(new WhiteListEntry(gameprofile));
	}

	public void removeWhitelist(GameProfile gameprofile) {
		whitelist.remove(gameprofile);
	}

	public WhiteList getWhitelist() {
		return whitelist;
	}

	public String[] getWhitelisted() {
		return whitelist.getEntries();
	}

	public OpList getOPs() {
		return operators;
	}

	public String[] n() {
		return operators.getEntries();
	}

	public void reloadWhitelist() {
	}

	public void b(EntityPlayer entityplayer, WorldServer worldserver) {
		entityplayer.playerConnection.sendPacket(new PacketPlayOutUpdateTime(worldserver.getTime(), worldserver.getDayTime(), worldserver.getGameRules().getBoolean("doDaylightCycle")));
		if (worldserver.Q()) {
			// CraftBukkit start - handle player weather
			// entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(1, 0.0F));
			// entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(7, worldserver.j(1.0F)));
			// entityplayer.playerConnection.sendPacket(new PacketPlayOutGameStateChange(8, worldserver.h(1.0F)));
			entityplayer.setPlayerWeather(org.bukkit.WeatherType.DOWNFALL, false);
			// CraftBukkit end
		}
	}

	public void updateClient(EntityPlayer entityplayer) {
		entityplayer.updateInventory(entityplayer.defaultContainer);
		entityplayer.getBukkitEntity().updateScaledHealth(); // CraftBukkit - Update scaled health on respawn and worldchange
		entityplayer.playerConnection.sendPacket(new PacketPlayOutHeldItemSlot(entityplayer.inventory.itemInHandIndex));
	}

	public int getPlayerCount() {
		return players.size();
	}

	public int getMaxPlayers() {
		return maxPlayers;
	}

	public String[] getSeenPlayers() {
		// CraftBukkit - fix reference to worldserver array
		return server.worlds.get(0).getDataManager().getPlayerFileData().getSeenPlayers();
	}

	public boolean getHasWhitelist() {
		return hasWhitelist;
	}

	public void setHasWhitelist(boolean flag) {
		hasWhitelist = flag;
	}

	public List b(String s) {
		ArrayList arraylist = new ArrayList();
		Iterator iterator = players.iterator();

		while (iterator.hasNext()) {
			EntityPlayer entityplayer = (EntityPlayer) iterator.next();

			if (entityplayer.s().equals(s)) {
				arraylist.add(entityplayer);
			}
		}

		return arraylist;
	}

	public int s() {
		return q;
	}

	public MinecraftServer getServer() {
		return server;
	}

	public NBTTagCompound t() {
		return null;
	}

	private void a(EntityPlayer entityplayer, EntityPlayer entityplayer1, World world) {
		if (entityplayer1 != null) {
			entityplayer.playerInteractManager.setGameMode(entityplayer1.playerInteractManager.getGameMode());
		} else if (r != null) {
			entityplayer.playerInteractManager.setGameMode(r);
		}

		entityplayer.playerInteractManager.b(world.getWorldData().getGameType());
	}

	public void u() {
		while (!players.isEmpty()) {
			// Spigot start
			EntityPlayer p = (EntityPlayer) players.get(0);
			p.playerConnection.disconnect(server.server.getShutdownMessage());
			if (!players.isEmpty() && players.get(0) == p) {
				players.remove(0); // Prevent shutdown hang if already disconnected
			}
			// Spigot end
		}
	}

	// CraftBukkit start - Support multi-line messages
	public void sendMessage(IChatBaseComponent[] ichatbasecomponent) {
		for (IChatBaseComponent component : ichatbasecomponent) {
			sendMessage(component, true);
		}
	}

	// CraftBukkit end

	public void sendMessage(IChatBaseComponent ichatbasecomponent, boolean flag) {
		server.sendMessage(ichatbasecomponent);
		sendAll(new PacketPlayOutChat(ichatbasecomponent, flag));
	}

	public void sendMessage(IChatBaseComponent ichatbasecomponent) {
		this.sendMessage(ichatbasecomponent, true);
	}

	public ServerStatisticManager a(EntityHuman entityhuman) {
		UUID uuid = entityhuman.getUniqueID();
		ServerStatisticManager serverstatisticmanager = uuid == null ? null : (ServerStatisticManager) n.get(uuid);

		if (serverstatisticmanager == null) {
			File file1 = new File(server.getWorldServer(0).getDataManager().getDirectory(), "stats");
			File file2 = new File(file1, uuid.toString() + ".json");

			if (!file2.exists()) {
				File file3 = new File(file1, entityhuman.getName() + ".json");

				if (file3.exists() && file3.isFile()) {
					file3.renameTo(file2);
				}
			}

			serverstatisticmanager = new ServerStatisticManager(server, file2);
			serverstatisticmanager.a();
			n.put(uuid, serverstatisticmanager);
		}

		return serverstatisticmanager;
	}

	public void a(int i) {
		q = i;
		if (server.worldServer != null) {
			WorldServer[] aworldserver = server.worldServer;
			int j = aworldserver.length;

			for (int k = 0; k < j; ++k) {
				WorldServer worldserver = aworldserver[k];

				if (worldserver != null) {
					worldserver.getPlayerChunkMap().a(i);
				}
			}
		}
	}
}
