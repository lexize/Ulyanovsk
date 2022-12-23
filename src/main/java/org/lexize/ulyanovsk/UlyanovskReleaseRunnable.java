package org.lexize.ulyanovsk;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.lexize.ulyanovsk.models.JailData;
import org.lexize.ulyanovsk.models.ReleaseData;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.function.Supplier;

public class UlyanovskReleaseRunnable extends BukkitRunnable {
    @Override
    public void run() {
        try {
            var db = Ulyanovsk.getInstance().getDatabase();
            List<JailData> dataList = db.GetJailDataToRelease(LocalDateTime.now().toEpochSecond(ZoneOffset.UTC));
            for (var data: dataList) {
                var uuid = data.getJailedPlayerUUID();
                var opl = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                var jailerPlayer = Bukkit.getOfflinePlayer(UUID.fromString(data.getInvokerUUID()));
                if (opl.isOnline()) {
                    var pl = opl.getPlayer();
                    Ulyanovsk.getInstance().RestorePlayerData(pl, data.getSavedPlayerData());
                }
                else {
                    db.AddToUnjailQueue(data);
                }
                db.AddRecordToHistory(new ReleaseData(data, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)));
                db.RemoveJailRecord(data);
                BaseComponent bc = Ulyanovsk.Utils.ParseLomponent(
                        Ulyanovsk.getInstance().getTranslation().getTranslation("player_released_auto"),
                        new HashMap<String, Supplier<String>>() {{
                            put("jailed_player", opl::getName);
                            put("jailer", jailerPlayer::getName);
                        }}
                );
                Ulyanovsk.getInstance().SendMessageToEveryoneWithPermission("ulyanovsk.event.release.auto.see", bc);
            }
        } catch (SQLException | IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
