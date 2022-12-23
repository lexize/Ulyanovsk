package org.lexize.ulyanovsk.event_handlers;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
}
