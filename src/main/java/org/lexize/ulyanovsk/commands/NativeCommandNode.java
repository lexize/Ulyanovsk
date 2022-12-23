package org.lexize.ulyanovsk.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandDispatcher;
import net.minecraft.commands.CommandListenerWrapper;

public class NativeCommandNode {
    public static LiteralArgumentBuilder<CommandListenerWrapper> literal(String name) {
        return CommandDispatcher.a(name);
    }

    public static RequiredArgumentBuilder<CommandListenerWrapper, ?> argument(String name, ArgumentType<?> type) {
        return CommandDispatcher.a(name, type);
    }
}
