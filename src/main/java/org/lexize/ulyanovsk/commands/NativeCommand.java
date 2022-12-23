package org.lexize.ulyanovsk.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import org.bukkit.command.CommandSender;

public interface NativeCommand {
    int run(CommandSender sender, CommandContext<?> context) throws CommandSyntaxException;
}
