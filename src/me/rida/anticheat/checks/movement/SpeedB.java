package me.rida.anticheat.checks.movement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import me.rida.anticheat.AntiCheat;
import me.rida.anticheat.checks.Check;
import me.rida.anticheat.checks.CheckType;
import me.rida.anticheat.utils.BlockUtil;
import me.rida.anticheat.utils.MathUtil;
import me.rida.anticheat.utils.PlayerUtil;

public class SpeedB extends Check {

	public static Map<UUID, Map.Entry<Integer, Long>> speedTicks;
	public static Map<UUID, Map.Entry<Integer, Long>> tooFastTicks;
	public static Map<UUID, Long> lastHit;

	public SpeedB(AntiCheat AntiCheat) {
		super("SpeedB", "Speed", CheckType.Movement, AntiCheat);
		
		setEnabled(true);
		setMaxViolations(15);
		setViolationResetTime(TimeUnit.MINUTES.toMillis(2));
        setBannable(true);
        setViolationsToNotify(4);
        
		SpeedB.lastHit = new HashMap<UUID, Long>();
		SpeedB.tooFastTicks = new HashMap<UUID, Map.Entry<Integer, Long>>();
		SpeedB.speedTicks = new HashMap<UUID, Map.Entry<Integer, Long>>();
	}

	@SuppressWarnings("deprecation")
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onMove(PlayerMoveEvent e) {

		Location from = e.getFrom().clone();
		Location to = e.getTo().clone();
		Player p = e.getPlayer();

		Location l = p.getLocation();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		Location blockLoc = new Location(p.getWorld(), x, y - 1, z);
		Location loc = new Location(p.getWorld(), x, y, z);
		Location loc2 = new Location(p.getWorld(), x, y + 1, z);
		Location above = new Location(p.getWorld(), x, y + 2, z);
		Location above3 = new Location(p.getWorld(), x - 1, y + 2, z - 1);
		long lastHitDiff = Math.abs(System.currentTimeMillis() - SpeedC.lastHit.getOrDefault(p.getUniqueId(), 0L));
		
		if ((e.getTo().getX() == e.getFrom().getX()) && (e.getTo().getZ() == e.getFrom().getZ())
				&& (e.getTo().getY() == e.getFrom().getY())
				|| lastHitDiff < 1500L 
				|| p.getNoDamageTicks() != 0
				|| p.getVehicle() != null
				|| p.getGameMode().equals(GameMode.CREATIVE)
				|| p.getAllowFlight()
				|| getAntiCheat().getLag().getTPS() < getAntiCheat().getTPSCancel()
		        || getAntiCheat().getLag().getPing(p) > getAntiCheat().getPingCancel()
           		|| BlockUtil.isNearIce(p)
   		        || BlockUtil.isNearSlime(p)
           		|| PlayerUtil.wasOnSlime(p)){
			return;
		}

		double Airmaxspeed = 0.4;
		double maxSpeed = 0.42;
		double newmaxspeed = 0.75;
		if (isOnIce(p)) {
			newmaxspeed = 1.0;
		}

		double ig = 0.28;
		double speed = MathUtil.offset(getHV(to.toVector()), getHV(from.toVector()));
		double onGroundDiff = (to.getY() - from.getY());

		if (p.hasPotionEffect(PotionEffectType.SPEED)) {
			int level = getPotionEffectLevel(p, PotionEffectType.SPEED);
			if (level > 0) {
				newmaxspeed = (newmaxspeed * (((level * 20) * 0.011) + 1));
				Airmaxspeed = (Airmaxspeed * (((level * 20) * 0.011) + 1));
				maxSpeed = (maxSpeed * (((level * 20) * 0.011) + 1));
				ig = (ig * (((level * 20) * 0.011) + 1));
			}
		}
		Airmaxspeed += p.getWalkSpeed() > 0.2 ? p.getWalkSpeed() * 0.8 : 0;
		maxSpeed += p.getWalkSpeed() > 0.2 ? p.getWalkSpeed() * 0.8 : 0;

		if (isReallyOnGround(p) && to.getY() == from.getY()) {
			if (speed >= maxSpeed && p.isOnGround() && p.getFallDistance() < 0.15
					&& blockLoc.getBlock().getType() != Material.ICE
					&& blockLoc.getBlock().getType() != Material.PACKED_ICE
					&& loc2.getBlock().getType() != Material.TRAP_DOOR && above.getBlock().getType() == Material.AIR
					&& above3.getBlock().getType() == Material.AIR) {
				getAntiCheat().logCheat(this, p, "On Ground", "(Type: B)");
			}
		}
			if (!isReallyOnGround(p) && speed >= Airmaxspeed && !isOnIce(p)
					&& blockLoc.getBlock().getType() != Material.ICE && !blockLoc.getBlock().isLiquid()
					&& !loc.getBlock().isLiquid() && blockLoc.getBlock().getType() != Material.PACKED_ICE
					&& above.getBlock().getType() == Material.AIR && above3.getBlock().getType() == Material.AIR
					&& blockLoc.getBlock().getType() != Material.AIR) {
				getAntiCheat().logCheat(this, p, "Mid Air", "(Type: B)");
			}
		if (speed >= newmaxspeed && isOnIce(p) && p.getFallDistance() < 0.6
				&& loc2.getBlock().getType() != Material.TRAP_DOOR && above.getBlock().getType() == Material.AIR
				&& loc2.getBlock().getType() == Material.AIR) {
			getAntiCheat().logCheat(this, p, "Limit", "(Type: B)");

		}

		if (speed > ig && !isAir(p) && onGroundDiff <= -0.4 && p.getFallDistance() <= 0.4
				&& !flaggyStuffNear(p.getLocation()) && blockLoc.getBlock().getType() != Material.ICE
				&& e.getTo().getY() != e.getFrom().getY() && blockLoc.getBlock().getType() != Material.PACKED_ICE
				&& loc2.getBlock().getType() != Material.TRAP_DOOR && above.getBlock().getType() == Material.AIR
				&& above3.getBlock().getType() == Material.AIR) {
			getAntiCheat().logCheat(this, p, "Vanilla", "(Type: B)");
		}
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	private void onPlayerQuit(PlayerQuitEvent e) {
		if (speedTicks.containsKey(e.getPlayer().getUniqueId())) {
			speedTicks.remove(e.getPlayer().getUniqueId());
		}
		if (tooFastTicks.containsKey(e.getPlayer().getUniqueId())) {
			tooFastTicks.remove(e.getPlayer().getUniqueId());
		}
		if (lastHit.containsKey(e.getPlayer().getUniqueId())) {
			lastHit.remove(e.getPlayer().getUniqueId());
		}
	}

	private boolean isOnIce(final Player player) {
		final Location a = player.getLocation();
		a.setY(a.getY() - 1.0);
		if (a.getBlock().getType().equals((Object) Material.ICE)) {
			return true;
		}
		a.setY(a.getY() - 1.0);
		return a.getBlock().getType().equals((Object) Material.ICE);
	}

	private int getPotionEffectLevel(Player p, PotionEffectType pet) {
		for (PotionEffect pe : p.getActivePotionEffects()) {
			if (pe.getType().getName().equals(pet.getName())) {
				return pe.getAmplifier() + 1;
			}
		}
		return 0;
	}

	private Vector getHV(Vector V) {
		V.setY(0);
		return V;
	}
	@SuppressWarnings("deprecation")
	private static boolean isReallyOnGround(Player p) {
		Location l = p.getLocation();
		int x = l.getBlockX();
		int y = l.getBlockY();
		int z = l.getBlockZ();
		Location b = new Location(p.getWorld(), x, y - 1, z);

		if (p.isOnGround() && b.getBlock().getType() != Material.AIR && b.getBlock().getType() != Material.WEB
				&& !b.getBlock().isLiquid()) {
			return true;
		} else {
			return false;
		}
	}
	
	private static boolean flaggyStuffNear(Location loc) {
		boolean nearBlocks = false;
		for (Block bl : BlockUtil.getSurrounding(loc.getBlock(), true)) {
			if ((bl.getType().equals(Material.STEP)) || (bl.getType().equals(Material.DOUBLE_STEP))
					|| (bl.getType().equals(Material.BED)) || (bl.getType().equals(Material.WOOD_DOUBLE_STEP))
					|| (bl.getType().equals(Material.WOOD_STEP))) {
				nearBlocks = true;
				break;
			}
		}
		for (Block bl : BlockUtil.getSurrounding(loc.getBlock(), false)) {
			if ((bl.getType().equals(Material.STEP)) || (bl.getType().equals(Material.DOUBLE_STEP))
					|| (bl.getType().equals(Material.BED)) || (bl.getType().equals(Material.WOOD_DOUBLE_STEP))
					|| (bl.getType().equals(Material.WOOD_STEP))) {
				nearBlocks = true;
				break;
			}
		}
		if (isBlock(loc.getBlock().getRelative(BlockFace.DOWN), new Material[] { Material.STEP, Material.BED,
				Material.DOUBLE_STEP, Material.WOOD_DOUBLE_STEP, Material.WOOD_STEP })) {
			nearBlocks = true;
		}
		return nearBlocks;
	}
	

	private static boolean isBlock(Block block, Material[] materials) {
		Material type = block.getType();
		Material[] arrayOfMaterial;
		int j = (arrayOfMaterial = materials).length;
		for (int i = 0; i < j; i++) {
			Material m = arrayOfMaterial[i];
			if (m == type) {
				return true;
			}
		}
		return false;
	}

	private static boolean isAir(final Player player) {
		final Block b = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		return b.getType().equals((Object) Material.AIR)
				&& b.getRelative(BlockFace.WEST).getType().equals((Object) Material.AIR)
				&& b.getRelative(BlockFace.NORTH).getType().equals((Object) Material.AIR)
				&& b.getRelative(BlockFace.EAST).getType().equals((Object) Material.AIR)
				&& b.getRelative(BlockFace.SOUTH).getType().equals((Object) Material.AIR);
	}
}