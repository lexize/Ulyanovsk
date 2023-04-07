package org.lexize.ulyanovsk.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.md_5.bungee.api.chat.BaseComponent;
import net.minecraft.commands.CommandListenerWrapper;
import org.bukkit.command.CommandSender;
import org.lexize.ulyanovsk.Ulyanovsk;
import org.lexize.ulyanovsk.UlyanovskTranslation;
import org.lexize.ulyanovsk.models.DataElement;

import java.util.HashMap;
import java.util.Map;

import static org.lexize.ulyanovsk.commands.NativeCommandNode.literal;
import static org.lexize.ulyanovsk.commands.NativeCommandNode.argument;

public class JailHistoryCommand {
    private static UlyanovskTranslation getTranslation() {
        return Ulyanovsk.getInstance().getTranslation();
    }
    private static LiteralArgumentBuilder<CommandListenerWrapper> _command;

    public static LiteralArgumentBuilder<CommandListenerWrapper> getCommand() {
        return _command;
    }

    static {
        _command = literal("jail_history");
        _command.then(literal("list").then(
                argument("offset", IntegerArgumentType.integer(0)).then(
                        argument("limit", IntegerArgumentType.integer(1, 15)).executes(JailHistoryCommand::_ListHistoryElements)
                ).executes(JailHistoryCommand::_ListHistoryElements)
        ).executes(JailHistoryCommand::_ListHistoryElements));
        _command.then(literal("get").then(argument("element_id", IntegerArgumentType.integer(0)).executes(
                JailHistoryCommand::_GetHistoryElement
        )));
    }

    private static int _ListHistoryElements(CommandContext<CommandListenerWrapper> ctx) {
        try {
            var db = Ulyanovsk.getInstance().getDatabase();
            CommandSender s = ctx.getSource().getBukkitSender();
            if (!s.hasPermission("ulyanovsk.command.history")) {
                BaseComponent c = Ulyanovsk.Utils.ParseMinimessage(getTranslation().getTranslation("no_permission"), null);
                s.spigot().sendMessage(c);
                return 0;
            }
            int offset = Ulyanovsk.Utils.argumentExistsInCtx(ctx, "offset") ? IntegerArgumentType.getInteger(ctx, "offset") : 0;
            int limit = Ulyanovsk.Utils.argumentExistsInCtx(ctx, "limit") ? IntegerArgumentType.getInteger(ctx, "limit") : 10;
            Map<Integer, DataElement> dataElements = db.GetRecordsFromHistory(limit, offset);
            for (Map.Entry<Integer, DataElement> kv :
                    dataElements.entrySet().stream().sorted((t1, t2) -> t2.getKey() - t1.getKey()).toList()) {
                s.spigot().sendMessage(kv.getValue().getShortMessage(kv.getKey()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private static int _GetHistoryElement(CommandContext<CommandListenerWrapper> ctx) {
        try {
            var db = Ulyanovsk.getInstance().getDatabase();
            CommandSender s = ctx.getSource().getBukkitSender();
            if (!s.hasPermission("ulyanovsk.command.history")) {
                BaseComponent c = Ulyanovsk.Utils.ParseMinimessage(getTranslation().getTranslation("no_permission"), null);
                s.spigot().sendMessage(c);
                return 0;
            }
            int elementId = IntegerArgumentType.getInteger(ctx, "element_id");
            DataElement element = db.GetRecordFromHistory(elementId);
            if (element == null) {
                BaseComponent c = Ulyanovsk.Utils.ParseMinimessage(getTranslation().getTranslation("history_element_not_found"), new HashMap<>(){{
                    put("element_id", () -> Integer.toString(elementId));
                }});
                s.spigot().sendMessage(c);
                return 0;
            }
            s.spigot().sendMessage(element.getMessage(elementId));
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return 0;
    }
}
