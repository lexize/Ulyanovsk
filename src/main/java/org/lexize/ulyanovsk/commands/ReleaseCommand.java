package org.lexize.ulyanovsk.commands;

import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.commands.CommandListenerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lexize.ulyanovsk.Ulyanovsk;
import org.lexize.ulyanovsk.UlyanovskTranslation;
import org.lexize.ulyanovsk.models.JailData;
import org.lexize.ulyanovsk.models.ReleaseData;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.lexize.ulyanovsk.commands.NativeCommandNode.literal;
import static org.lexize.ulyanovsk.commands.NativeCommandNode.argument;

public class ReleaseCommand {
    private static UlyanovskTranslation getTranslation() {
        return Ulyanovsk.getInstance().getTranslation();
    }
    private static LiteralArgumentBuilder<CommandListenerWrapper> _command;
    public static LiteralArgumentBuilder<CommandListenerWrapper> getCommand() {
        return _command;
    }
    static {
        _command = literal("release");
        _command.then(literal("by_case_id").then(
                argument("case_id", IntegerArgumentType.integer(0)).suggests(ReleaseCommand::SuggestCaseIDs).executes(ReleaseCommand::ReleaseCmd).then(
                        argument("reason", StringArgumentType.string()).executes(ReleaseCommand::ReleaseCmd)
                )
        ));
        _command.then(literal("by_name").then(
                argument("player_name", StringArgumentType.word()).suggests(ReleaseCommand::SuggestPlayerNames).executes(ReleaseCommand::ReleaseCmd)
                        .then(argument("reason", StringArgumentType.string()).executes(ReleaseCommand::ReleaseCmd))
                )
        );
    }

    private static int ReleaseCmd(CommandContext<CommandListenerWrapper> ctx) {
        try {
            CommandSender s = ctx.getSource().getBukkitSender();
            if (!s.hasPermission("ulyanovsk.command.release")) {
                BaseComponent c = Ulyanovsk.Utils.ParseMinimessage(getTranslation().getTranslation("no_permission"), null);
                s.spigot().sendMessage(c);
                return 0;
            }
            JailData dataToRelease = null;
            if (Ulyanovsk.Utils.argumentExistsInCtx(ctx, "by_case_id")) {
                int caseID = IntegerArgumentType.getInteger(ctx, "case_id");
                dataToRelease = Ulyanovsk.getInstance().getDatabase().GetJailData(caseID);
                if (dataToRelease == null) {
                    BaseComponent c = Ulyanovsk.Utils.ParseMinimessage(getTranslation().getTranslation("case_id_not_found"), new HashMap<>() {{
                        put("case_id", () -> Integer.toString(caseID));
                    }});
                    s.spigot().sendMessage(c);
                    return 0;
                }
            }
            else {
                String playerName = StringArgumentType.getString(ctx, "player_name");
                for (JailData data :
                        Ulyanovsk.getInstance().getDatabase().GetJailData()) {
                    OfflinePlayer opl = Bukkit.getOfflinePlayer(UUID.fromString(data.getJailedPlayerUUID()));
                    if (opl.getName().toLowerCase().equals(playerName.toLowerCase())) {
                        dataToRelease = data;
                        break;
                    }
                }
                if (dataToRelease == null) {
                    BaseComponent c = Ulyanovsk.Utils.ParseMinimessage(getTranslation().getTranslation("player_not_found"), new HashMap<>() {{
                        put("jailed_player", () -> playerName);
                    }});
                    s.spigot().sendMessage(c);
                    return 0;
                }
            }
            String reason = Ulyanovsk.Utils.argumentExistsInCtx(ctx, "reason") ? StringArgumentType.getString(ctx, "reason") :
                    getTranslation().getTranslation("no_reason");
            _ReleasePlayer(dataToRelease, reason, s);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static CompletableFuture<Suggestions> SuggestCaseIDs
            (CommandContext<CommandListenerWrapper> context, SuggestionsBuilder sb) {
        var database = Ulyanovsk.getInstance().getDatabase();
        boolean isCaseProvided = Ulyanovsk.Utils.argumentExistsInCtx(context, "case_id");
        String caseId = isCaseProvided ? Integer.toString(IntegerArgumentType.getInteger(context, "case_id")) : null;
        try {
            for (JailData jd :
                    database.GetJailData()) {
                String ccid = Integer.toString(jd.getCaseID());
                if (caseId == null || ccid.startsWith(caseId)) {
                    OfflinePlayer ofp = Bukkit.getOfflinePlayer(UUID.fromString(jd.getJailedPlayerUUID()));
                    sb.suggest(ccid, new LiteralMessage(ofp.getName()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return sb.buildFuture();
    }

    private static CompletableFuture<Suggestions> SuggestPlayerNames
            (CommandContext<CommandListenerWrapper> context, SuggestionsBuilder sb) {
        var database = Ulyanovsk.getInstance().getDatabase();
        boolean isCaseProvided = Ulyanovsk.Utils.argumentExistsInCtx(context, "player_name");
        String playerName = isCaseProvided ? StringArgumentType.getString(context, "player_name") : null;
        try {
            for (JailData jd :
                    database.GetJailData()) {
                OfflinePlayer ofp = Bukkit.getOfflinePlayer(UUID.fromString(jd.getJailedPlayerUUID()));
                String currentPlayerName = ofp.getName();
                if (playerName == null || currentPlayerName.startsWith(playerName)) {
                    sb.suggest(currentPlayerName, new LiteralMessage("Case id: %s".formatted(jd.getCaseID())));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return sb.buildFuture();
    }

    private static void _ReleasePlayer(JailData data, String reason, CommandSender releaser)
            throws IOException, ClassNotFoundException, SQLException {
        var db = Ulyanovsk.getInstance().getDatabase();
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
        String invokerID;
        if (releaser instanceof Player pl) {
            invokerID = pl.getUniqueId().toString();
        }
        else invokerID = releaser.getName();
        db.AddRecordToHistory(new ReleaseData(data, LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), reason, invokerID));
        db.RemoveJailRecord(data);
        BaseComponent bc = Ulyanovsk.Utils.ParseMinimessage(
                Ulyanovsk.getInstance().getTranslation().getTranslation("player_released_manual"),
                new HashMap<String, Supplier<String>>() {{
                    put("jailed_player", opl::getName);
                    put("jailer", jailerPlayer::getName);
                    put("releaser", releaser::getName);
                    put("reason", () -> reason);
                }}
        );
        Ulyanovsk.getInstance().SendMessageToEveryoneWithPermission("ulyanovsk.event.release.manual.see", bc);
    }
}
