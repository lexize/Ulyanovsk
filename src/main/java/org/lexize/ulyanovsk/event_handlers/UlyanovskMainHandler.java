package org.lexize.ulyanovsk.event_handlers;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.lexize.ulyanovsk.Ulyanovsk;
import org.lexize.ulyanovsk.models.JailedPlayerSavedData;

import java.io.IOException;
import java.sql.SQLException;

public class UlyanovskMainHandler implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        try {
            var db = Ulyanovsk.getInstance().getDatabase();
            var pl = event.getPlayer();
            if (db.IsInUnjailQueue(pl.getUniqueId())) {
                JailedPlayerSavedData savedData = db.GetSavedDataFromUnjailQueue(pl.getUniqueId());
                Ulyanovsk.getInstance().RestorePlayerData(pl, savedData);
                db.RemoveFromUnjailQueue(pl.getUniqueId());
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @EventHandler
    public void onPlayerDeath(EntityDamageEvent event) {
        try {
            var inst = Ulyanovsk.getInstance();
            var db = inst.getDatabase();
            var pl = event.getEntity();
            if (pl instanceof Player le) {
                if ((le.getHealth() - event.getDamage() <= 0) && db.IsPlayerInJail(pl.getUniqueId().toString())) {
                    event.setCancelled(true);
                    le.setHealth(le.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                    var tpos = inst.getConfiguration().TeleportPosition;
                    var location = new Location(inst.getJailWorld(), tpos.x,tpos.y,tpos.z,tpos.yaw,tpos.pitch);
                    le.teleport(location);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
