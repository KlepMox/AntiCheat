package me.rida.anticheat.checks.combat;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

import me.rida.anticheat.checks.Check;
import me.rida.anticheat.utils.Color;
import me.rida.anticheat.AntiCheat;

public class HitBoxB
extends Check {
    private double HITBOX_LENGTH = 1.05;

    public HitBoxB(AntiCheat AntiCheat) {
        super("HitBoxB", "HitBox", AntiCheat);
		setEnabled(true);
		setMaxViolations(10);
		setBannable(false);
		setViolationsToNotify(1);
    }

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onHitPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) {
            return;
        }
        Player p = (Player)e.getDamager();
        Player p2 = (Player)e.getDamager();
        if (!this.hasInHitBox((LivingEntity)p2)) {
        	getAntiCheat().logCheat(this, p, Color.Red + "Experemental", "(Type: B)");
             }
    }

    public boolean hasInHitBox(LivingEntity e) {
        boolean bl = false;
        Vector vector = e.getLocation().toVector().subtract(e.getLocation().toVector());
        Vector vector2 = e.getLocation().toVector().subtract(e.getLocation().toVector());
        if (!(e.getLocation().getDirection().normalize().crossProduct(vector).lengthSquared() >= this.HITBOX_LENGTH && e.getLocation().getDirection().normalize().crossProduct(vector2).lengthSquared() >= this.HITBOX_LENGTH || vector.normalize().dot(e.getLocation().getDirection().normalize()) < 0.0 && vector2.normalize().dot(e.getLocation().getDirection().normalize()) < 0.0)) {
            bl = true;
        }
        return bl;
    }
}

