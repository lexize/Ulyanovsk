package org.lexize.ulyanovsk.models;

import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.lexize.lomponent.LomponentSerializer;
import org.lexize.ulyanovsk.Ulyanovsk;
import org.lexize.ulyanovsk.annotations.SQLAutoincrement;
import org.lexize.ulyanovsk.annotations.SQLPrimaryKey;
import org.lexize.ulyanovsk.annotations.SQLValue;

import java.util.HashMap;
import java.util.UUID;

public class JailData extends DataElement {
    @SQLPrimaryKey
    @SQLAutoincrement
    @SQLValue("case_id")
    private int _caseId;
    @SQLValue("player_id")
    private String _jailedPlayerUUID;
    @SQLValue("jail_start_time")
    private long _jailStartTime;
    @SQLValue("jail_length")
    private long _jailLength;
    @SQLValue("saved_player_data")
    private JailedPlayerSavedData _savedPlayerData;
    @SQLValue("reason")
    private String _reason;
    protected JailData() {
        super();
    }

    @Override
    public BaseComponent getShortMessage(int element_id) {
        var ulyanovsk = Ulyanovsk.getInstance();
        OfflinePlayer jailedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(_jailedPlayerUUID));
        OfflinePlayer jailer;
        try {
            jailer = Bukkit.getOfflinePlayer(UUID.fromString(getInvokerUUID()));
        }
        catch (Exception ignored) {
            jailer = null;
        }
        OfflinePlayer finalJailer = jailer;
        return Ulyanovsk.Utils.ParseLomponent(ulyanovsk.getTranslation().getTranslation("short_history_jailed"), new HashMap<>() {{
            put("element_id", () -> Integer.toString(element_id));
            put("jailed_player", jailedPlayer::getName);
            put("jailer",() -> finalJailer != null ? finalJailer.getName() : getInvokerUUID());
            put("jail_length",() -> Ulyanovsk.Utils.GetTimestampString(_jailLength));
            put("jail_length_total",() -> Long.toString(_jailLength));
            put("jail_start_time",() -> Ulyanovsk.Utils.GetDatetimeString(_jailStartTime));
            put("date", () -> Ulyanovsk.Utils.GetDateString(getCreationTime()));
            put("time", () -> Ulyanovsk.Utils.GetTimeString(getCreationTime()));
            put("datetime", () -> Ulyanovsk.Utils.GetDatetimeString(getCreationTime()));
            put("reason",() -> _reason);
            put("case_id",() -> Integer.toString(_caseId));
        }});
    }

    @Override
    public BaseComponent getMessage(int element_id) {
        var ulyanovsk = Ulyanovsk.getInstance();
        OfflinePlayer jailedPlayer = Bukkit.getOfflinePlayer(UUID.fromString(_jailedPlayerUUID));
        OfflinePlayer jailer;
        try {
            jailer = Bukkit.getOfflinePlayer(UUID.fromString(getInvokerUUID()));
        }
        catch (Exception ignored) {
            jailer = null;
        }
        OfflinePlayer finalJailer = jailer;
        return Ulyanovsk.Utils.ParseLomponent(ulyanovsk.getTranslation().getTranslation("history_jail_data"), new HashMap<>() {{
            put("element_id", () -> Integer.toString(element_id));
            put("jailed_player", jailedPlayer::getName);
            put("jailer",() -> finalJailer != null ? finalJailer.getName() : getInvokerUUID());
            put("jail_length",() -> Ulyanovsk.Utils.GetTimestampString(_jailLength));
            put("jail_length_total",() -> Long.toString(_jailLength));
            put("jail_start_time",() -> Ulyanovsk.Utils.GetDatetimeString(_jailStartTime));
            put("date", () -> Ulyanovsk.Utils.GetDateString(getCreationTime()));
            put("time", () -> Ulyanovsk.Utils.GetTimeString(getCreationTime()));
            put("datetime", () -> Ulyanovsk.Utils.GetDatetimeString(getCreationTime()));
            put("reason",() -> _reason);
            put("case_id",() -> Integer.toString(_caseId));
        }});
    }

    public JailData(String invokerUUID, long creationTime,
                    String jailedPlayerUUID, long jailStartTime, long jailLength, String reason, JailedPlayerSavedData savedPlayerData) {
        super(invokerUUID, creationTime);
        _jailedPlayerUUID = jailedPlayerUUID;
        _jailStartTime = jailStartTime;
        _jailLength = jailLength;
        _savedPlayerData = savedPlayerData;
        _reason = reason;
    }

    public String getJailedPlayerUUID() {
        return _jailedPlayerUUID;
    }


    public long getJailStartTime() {
        return _jailStartTime;
    }

    public long getJailLength() {
        return _jailLength;
    }
    public int getCaseID() {return _caseId;}
    public void setCaseID(int i) {
        _caseId = i;
    }
    public String getReason() {
        return _reason;
    }
    public JailedPlayerSavedData getSavedPlayerData() {return _savedPlayerData;}

    @Override
    public String toString() {
        return Ulyanovsk.getInstance().getJson().toJson(this);
    }
}
