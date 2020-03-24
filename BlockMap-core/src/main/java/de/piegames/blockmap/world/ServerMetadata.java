package de.piegames.blockmap.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ServerMetadata {
	/** This will be used in the future to keep track of old serialized files. */
	int							version	= 0;
	public Optional<String>		name;
	public Optional<String>		description;
	public Optional<String>		ipAddress;
	public Optional<String>		iconLocation;
	public Optional<Set<String>> onlinePlayers;
	public List<ServerLevel>	levels	= new ArrayList<>();
	public int maxPlayers;

	public ServerMetadata() {
		this(null, null, null, null, null, -1);
	}

	public ServerMetadata(String name, String description, String ipAddress, String iconLocation, Set<String> onlinePlayers, int maxPlayers) {
		this.name = Optional.ofNullable(name);
		this.description = Optional.ofNullable(description);
		this.ipAddress = Optional.ofNullable(ipAddress);
		this.iconLocation = Optional.ofNullable(iconLocation);
		this.onlinePlayers = Optional.ofNullable(onlinePlayers);
		this.maxPlayers = maxPlayers;
	}

	public static class ServerLevel {
		public String	name;
		public String	path;

		public ServerLevel() {

		}

		public ServerLevel(String name, String path) {
			this.name = name;
			this.path = path;
		}
	}
}