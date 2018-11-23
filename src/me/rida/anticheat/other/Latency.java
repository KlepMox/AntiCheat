package me.rida.anticheat.other;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import me.rida.anticheat.AntiCheat;
import me.rida.anticheat.packets.PacketPlayerType;
import me.rida.anticheat.packets.events.PacketPlayerEvent;
import me.rida.anticheat.utils.TimeUtil;

public class Latency implements Listener {

	public static Map<UUID, Map.Entry<Integer, Long>> packetTicks;
	public static Map<UUID, Long> lastPacket;
	public List<UUID> blacklist;
	private static Map<UUID, Integer> packets;

	private AntiCheat Ping;
	@SuppressWarnings("unused")
	private double tps;

	public Latency(AntiCheat AntiCheat) {
		this.Ping = AntiCheat;

		packetTicks = new HashMap<UUID, Map.Entry<Integer, Long>>();
		lastPacket = new HashMap<UUID, Long>();
		blacklist = new ArrayList<UUID>();
		packets = new HashMap<UUID, Integer>();
	}

	public static Integer getLag(Player p) {
		if (packets.containsKey(p.getUniqueId())) {
			return packets.get(p.getUniqueId());
		}
		return 0;
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void PlayerJoin(PlayerJoinEvent e) {
		Player p = e.getPlayer();
		UUID u = p.getUniqueId();
		this.blacklist.add(u);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onLogout(PlayerQuitEvent e) {
		Player p = e.getPlayer();
		UUID u = p.getUniqueId();
		if (packetTicks.containsKey(u)) {
			packetTicks.remove(u);
		}
		if (lastPacket.containsKey(u)) {
			lastPacket.remove(u);
		}
		if (blacklist.contains(u)) {
			blacklist.remove(u);
		}
		if (packets.containsKey(u)) {
			packets.remove(u);
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void PlayerChangedWorld(PlayerChangedWorldEvent e) {
		Player p = e.getPlayer();
		UUID u = p.getUniqueId();
		this.blacklist.add(u);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void PlayerRespawn(PlayerRespawnEvent e) {
		Player p = e.getPlayer();
		UUID u = p.getUniqueId();
		this.blacklist.add(u);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void PacketPlayer(PacketPlayerEvent e) {
		Player p = e.getPlayer();
		UUID u = p.getUniqueId();
		if (!Ping.isEnabled()) {
			return;
		}
		if (p.getGameMode().equals(GameMode.CREATIVE)
				|| Ping.lag.getTPS() > 21.0D 
				|| Ping.lag.getTPS() < Ping.getTPSCancel()
				|| e.getType() != PacketPlayerType.FLYING) {
			return;
		}
		int Count = 0;
		long Time = System.currentTimeMillis();
		if (Latency.packetTicks.containsKey(u)) {
			Count = Latency.packetTicks.get(u).getKey().intValue();
			Time = Latency.packetTicks.get(u).getValue().longValue();
		}
		if (Latency.lastPacket.containsKey(u)) {
			long MS = System.currentTimeMillis() - Latency.lastPacket.get(u).longValue();
			if (MS >= 100L) {
				this.blacklist.add(u);
			} else if ((MS > 1L) && (this.blacklist.contains(u))) {
				this.blacklist.remove(u);
			}
		}
		if (!this.blacklist.contains(u)) {
			Count++;
			if ((Latency.packetTicks.containsKey(u)) && (TimeUtil.elapsed(Time, 1000L))) {
				packets.put(u, Count);
				Count = 0;
				Time = TimeUtil.nowlong();
			}
		}
		Latency.packetTicks.put(u, new AbstractMap.SimpleEntry<Integer, Long>(Count, Time));
		Latency.lastPacket.put(u, System.currentTimeMillis());
	}

}