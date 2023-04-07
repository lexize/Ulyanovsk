package org.lexize.ulyanovsk.models;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.lexize.ulyanovsk.Ulyanovsk;

import java.util.HashMap;
import java.util.UUID;

public class ReleaseData extends DataElement {
    public enum ReleaseReason {
        AUTO,
        MANUAL
    }
    public long wasJailedFor;
    public long jailStart;
    public int jailCaseID;
    public String jailedUUID;
    public String jailerUUID;
    public String jailReason;
    public String manualReleaseReason;
    public ReleaseReason releaseReason;
    protected ReleaseData() {}

    public ReleaseData(JailData jail, long creationTime) {
        super("CONSOLE", creationTime);
        wasJailedFor = jail.getJailLength();
        jailStart = jail.getJailStartTime();
        jailCaseID = jail.getCaseID();
        jailedUUID = jail.getJailedPlayerUUID();
        jailerUUID = jail.getInvokerUUID();
        jailReason = jail.getReason();
        releaseReason = ReleaseReason.AUTO;
    }

    public ReleaseData(JailData jail, long creationTime, String releaseReason, String releaseInvoker) {
        super(releaseInvoker,creationTime);
        wasJailedFor = jail.getJailLength();
        jailStart = jail.getJailStartTime();
        jailCaseID = jail.getCaseID();
        jailedUUID = jail.getJailedPlayerUUID();
        jailerUUID = jail.getInvokerUUID();
        jailReason = jail.getReason();
        this.releaseReason = ReleaseReason.MANUAL;
        manualReleaseReason = releaseReason;
    }

    @Override
    public BaseComponent getShortMessage(int element_id) {
        var ulyanovsk = Ulyanovsk.getInstance();
        OfflinePlayer jailedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(jailedUUID));
        OfflinePlayer jailer;
        try {
            jailer = Bukkit.getOfflinePlayer(UUID.fromString(jailerUUID));
        }
        catch (Exception ignored) {
            jailer = null;
        }
        OfflinePlayer finalJailer = jailer;
        OfflinePlayer releaser;
        try {
            releaser = Bukkit.getOfflinePlayer(UUID.fromString(getInvokerUUID()));
        }
        catch (Exception ignored) {
            releaser = null;
        }
        OfflinePlayer finalReleaser = releaser;
        return Ulyanovsk.Utils.ParseMinimessage(ulyanovsk.getTranslation().getTranslation("short_history_released"), new HashMap<>() {{
            put("element_id", () -> Integer.toString(element_id));
            put("jailed_player", jailedPlayer::getName);
            put("jailer",() -> finalJailer != null ? finalJailer.getName() : jailerUUID);
            put("jail_length",() -> Ulyanovsk.Utils.GetTimestampString(wasJailedFor));
            put("jail_length_total",() -> Long.toString(wasJailedFor));
            put("jail_start_time",() -> Ulyanovsk.Utils.GetDatetimeString(jailStart));
            put("date", () -> Ulyanovsk.Utils.GetDateString(getCreationTime()));
            put("time", () -> Ulyanovsk.Utils.GetTimeString(getCreationTime()));
            put("datetime", () -> Ulyanovsk.Utils.GetDatetimeString(getCreationTime()));
            put("reason",() -> jailReason);
            put("case_id",() -> Integer.toString(jailCaseID));
            put("releaser", () -> finalReleaser != null ? finalReleaser.getName() : getInvokerUUID());
        }});
    }

    @Override
    public BaseComponent getMessage(int element_id) {
        var ulyanovsk = Ulyanovsk.getInstance();
        OfflinePlayer jailedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(jailedUUID));
        OfflinePlayer jailer;
        try {
            jailer = Bukkit.getOfflinePlayer(UUID.fromString(jailerUUID));
        }
        catch (Exception ignored) {
            jailer = null;
        }
        OfflinePlayer finalJailer = jailer;
        OfflinePlayer releaser;
        try {
            releaser = Bukkit.getOfflinePlayer(UUID.fromString(getInvokerUUID()));
        }
        catch (Exception ignored) {
            releaser = null;
        }
        OfflinePlayer finalReleaser = releaser;
        return Ulyanovsk.Utils.ParseMinimessage(ulyanovsk.getTranslation().getTranslation("history_released_data"), new HashMap<>() {{
            put("element_id", () -> Integer.toString(element_id));
            put("jailed_player", jailedPlayer::getName);
            put("jailer",() -> finalJailer != null ? finalJailer.getName() : jailerUUID);
            put("jail_length",() -> Ulyanovsk.Utils.GetTimestampString(wasJailedFor));
            put("jail_length_total",() -> Long.toString(wasJailedFor));
            put("jail_start_time",() -> Ulyanovsk.Utils.GetDatetimeString(jailStart));
            put("date", () -> Ulyanovsk.Utils.GetDateString(getCreationTime()));
            put("time", () -> Ulyanovsk.Utils.GetTimeString(getCreationTime()));
            put("datetime", () -> Ulyanovsk.Utils.GetDatetimeString(getCreationTime()));
            put("reason",() -> jailReason);
            put("case_id",() -> Integer.toString(jailCaseID));
            put("releaser", () -> finalReleaser != null ? finalReleaser.getName() : getInvokerUUID());
        }});
    }
}
