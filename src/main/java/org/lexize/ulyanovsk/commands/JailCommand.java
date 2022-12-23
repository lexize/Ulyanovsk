package org.lexize.ulyanovsk.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.commands.CommandListenerWrapper;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.lexize.ulyanovsk.Ulyanovsk;
import org.lexize.ulyanovsk.UlyanovskConfig;
import org.lexize.ulyanovsk.UlyanovskTranslation;
import org.lexize.ulyanovsk.exceptions.TimestampKeyNotFound;
import org.lexize.ulyanovsk.exceptions.TimestampNotMatches;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.lexize.ulyanovsk.Ulyanovsk.Utils.GetTimestampString;

public class JailCommand {
    public static final String TimestampCorrectCheckPattern = "^(?:\\d+\\D+)+$";
    public static final String TimestampComponentPattern = "(\\d+)(\\D+)";
    private static LiteralArgumentBuilder<CommandListenerWrapper> _command;

    public static LiteralArgumentBuilder<CommandListenerWrapper> getCommand() {
        return _command;
    }
    private static UlyanovskTranslation getTranslation() {
        return Ulyanovsk.getInstance().getTranslation();
    }

    static {
        UlyanovskConfig cfg = Ulyanovsk.getInstance().getConfiguration();
        _command = NativeCommandNode.literal("jail");
        _command.then(NativeCommandNode.argument(
                "player_name", StringArgumentType.word()
        ).suggests(JailCommand::SuggestPlayers).then(
                NativeCommandNode.argument("time", StringArgumentType.word()).suggests(JailCommand::SuggestTimestamp).executes(
                        JailCommand::JailTimedNoReason
                ).then(
                        NativeCommandNode.argument("reason", StringArgumentType.string()).executes(JailCommand::JailTimed)
                )
        ));
        //var jail_command = NativeCommandNode.literal("put").then();
        //var unjail_command = NativeCommandNode.literal("release").then(
        //        NativeCommandNode.argument("case_id", LongArgumentType.longArg(0))
        //).then(
        //        NativeCommandNode.argument("player_name", StringArgumentType.word()).suggests(JailCommand::SuggestJailedPlayers)
        //);
        //_command.then(jail_command);
        //_command.then(unjail_command);
        //_command.then(NativeCommandNode.literal("visit").executes(JailCommand::VisitJail));
    }

    private static int JailTimed(CommandContext<CommandListenerWrapper> context) {
        try {
            CommandSender s = context.getSource().getBukkitSender();
            if (!s.hasPermission("ulyanovsk.command.jail")) {
                BaseComponent c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("no_permission"), null);
                s.spigot().sendMessage(c);
                return 0;
            }
            String playerName = StringArgumentType.getString(context, "player_name");
            String reason = StringArgumentType.getString(context, "reason");
            String timestampString = StringArgumentType.getString(context, "time");
            long time = Ulyanovsk.Utils.GetTimeByTimestampData(Ulyanovsk.Utils.GetTimestampData(timestampString));
            boolean permanent = time <= 0;
            Player playerToJail = Bukkit.getPlayer(playerName);
            if (playerToJail == null) {
                BaseComponent c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("player_not_found"), new HashMap<>() {{
                    put("jailed_player", () -> playerName);
                }});
                s.spigot().sendMessage(c);
                return 0;
            }
            Ulyanovsk.getInstance().JailPlayer(s, playerToJail, permanent ? -1: time, reason);
            BaseComponent c;
            if (!permanent) {
                 c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("player_jailed"), new HashMap<>() {{
                    put("jailed_player", () -> playerName);
                    put("jailer", s::getName);
                    put("reason", () -> reason);
                    put("jail_length_total", () -> Long.toString(time));
                    put("jail_length", () -> GetTimestampString(time));
                }});
            }
            else {
                 c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("player_jailed_permanently"), new HashMap<>() {{
                    put("jailed_player", () -> playerName);
                    put("jailer", s::getName);
                    put("reason",  () -> reason);
                }});
            }
            Ulyanovsk.getInstance().SendMessageToEveryoneWithPermission("ulyanovsk.event.jail.see", c);
        } catch (TimestampNotMatches | TimestampKeyNotFound e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
    private static int JailTimedNoReason(CommandContext<CommandListenerWrapper> context) {
        try {
            CommandSender s = context.getSource().getBukkitSender();
            if (!s.hasPermission("ulyanovsk.command.jail")) {
                BaseComponent c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("no_permission"), null);
                s.spigot().sendMessage(c);
                return 0;
            }
            String playerName = StringArgumentType.getString(context, "player_name");
            String reason = getTranslation().getTranslation("no_reason");
            String timestampString = StringArgumentType.getString(context, "time");
            long time = Ulyanovsk.Utils.GetTimeByTimestampData(Ulyanovsk.Utils.GetTimestampData(timestampString));
            boolean permanent = time <= 0;
            Player playerToJail = Bukkit.getPlayer(playerName);
            if (playerToJail == null) {
                BaseComponent c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("player_not_found"), new HashMap<>() {{
                    put("jailed_player",  () -> playerName);
                }});
                s.spigot().sendMessage(c);
                return 0;
            }
            Ulyanovsk.getInstance().JailPlayer(s, playerToJail, permanent ? -1: time, reason);
            BaseComponent c;
            if (!permanent) {
                c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("player_jailed"), new HashMap<>() {{
                    put("jailed_player", () -> playerName);
                    put("jailer", s::getName);
                    put("reason", () -> reason);
                    put("jail_length_total", () -> Long.toString(time));
                    put("jail_length", () -> GetTimestampString(time));
                }});
            }
            else {
                c = Ulyanovsk.Utils.ParseLomponent(getTranslation().getTranslation("player_jailed_permanent"), new HashMap<>() {{
                    put("jailed_player", () -> playerName);
                    put("jailer", s::getName);
                    put("reason", () -> reason);
                }});
            }
            Ulyanovsk.getInstance().SendMessageToEveryoneWithPermission("ulyanovsk.event.jail.see", c);
        } catch (TimestampNotMatches | TimestampKeyNotFound e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
    private static CompletableFuture<Suggestions> SuggestPlayers(CommandContext<CommandListenerWrapper> context, SuggestionsBuilder sb) {
        var db = Ulyanovsk.getInstance().getDatabase();
        String playerName = Ulyanovsk.Utils.argumentExistsInCtx(context, "player_name") ?
                StringArgumentType.getString(context, "player_name") : null;
        for (Player p:
                Bukkit.getOnlinePlayers()) {
            String currentPlayerName = p.getName();
            if ((playerName == null || currentPlayerName.contains(playerName)) && !db.IsPlayerInJail(currentPlayerName)) {
                sb.suggest(currentPlayerName);
            }
        }
        return sb.buildFuture();
    }
    private static CompletableFuture<Suggestions> SuggestTimestamp(CommandContext<CommandListenerWrapper> context, SuggestionsBuilder sb) {
        UlyanovskConfig cfg = Ulyanovsk.getInstance().getConfiguration();
        String timestampInput = Ulyanovsk.Utils.argumentExistsInCtx(context, "time") ?
                StringArgumentType.getString(context, "time") : null;
        List<String> suggestions = new ArrayList<>();
        for (String s:
             Ulyanovsk.Utils.AvailableTimestampKeys) {
            suggestions.add("1"+s);
        }
        suggestions.add("60sec");
        suggestions.add("60min");
        String[] empty = new String[0];
        for (String a :
                cfg.TimestampComponentAliases.getOrDefault("sec", empty)) {
            suggestions.add("1"+a);
            suggestions.add("60"+a);
        }
        for (String a :
                cfg.TimestampComponentAliases.getOrDefault("min", empty)) {
            suggestions.add("1"+a);
            suggestions.add("60"+a);
        }
        suggestions.add("24hours");
        for (String a :
                cfg.TimestampComponentAliases.getOrDefault("hours", empty)) {
            suggestions.add("1"+a);
            suggestions.add("24"+a);
        }
        suggestions.add("30days");
        for (String a :
                cfg.TimestampComponentAliases.getOrDefault("days", empty)) {
            suggestions.add("1"+a);
            suggestions.add("30"+a);
        }
        for (String a :
                cfg.TimestampComponentAliases.getOrDefault("months", empty)) {
            suggestions.add("1"+a);
        }
        for (String s :
                suggestions) {
            if (timestampInput == null || s.contains(timestampInput)) {
                sb.suggest(s);
            }
        }
        return sb.buildFuture();
    }
}
