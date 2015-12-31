package multitallented.redcastlemedia.bukkit.townships;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;

import multitallented.redcastlemedia.bukkit.townships.region.SuperRegionType;

public class Charter {
	private SuperRegionType srt;
	private List<OfflinePlayer> members;
	
	public Charter(SuperRegionType srt, OfflinePlayer owner) {
		this.srt = srt;
		members = new ArrayList<OfflinePlayer>();
		members.add(owner);
	}
	
	public Charter(SuperRegionType srt, List<OfflinePlayer> members) {
		this.srt = srt;
		this.members = members;
	}
	
	public boolean addMember(OfflinePlayer member) {
		return members.add(member);
	}
	
	public SuperRegionType getSuperRegionType() {
		return srt;
	}
	
	public List<OfflinePlayer> getMembers() {
		return members;
	}
}
