package com.mnight.cppsqript.parser;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class ScriptErrorTracker {
    private static final List<String> errors = new ArrayList<>();
    private static final List<String> warnings = new ArrayList<>();
    private static boolean hasReloadedAtLeastOne = false;

    public static void clear(){
        errors.clear();
        hasReloadedAtLeastOne = true;
    }

    public static void reportError(String relativeFilePath, int line, int charPos, String massege){
        String formatted = String.format("[%s:%d:%d] %s", relativeFilePath, line, massege);
        errors.add(formatted);
        System.err.println("[CppSqript Error] " + formatted);
    }

    public static void reportRuntimeError(String relativeFilePath, int line, String message){
        String formatted = String.format("[%s:%d] %s", relativeFilePath, line, message);
        errors.add(formatted);
        System.err.println("[CppSqript Runtime Error] " + formatted);
    }

    public static void reportWarning(String context, String message) {
        String formatted = String.format("[%s] %s", context, message);
        warnings.add(formatted);
        System.out.println("[CppSqript Warning] " + formatted);
    }

    public static boolean hasErrors(){
        return !errors.isEmpty();
    }

    public static void broadcastToPlayer(ServerPlayer player){
        if (!hasReloadedAtLeastOne || player == null){
            return ;
        }

        if (errors.isEmpty()) {
            player.sendSystemMessage(
                Component.literal("[CppSqript] ").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .append(Component.literal("Successfully loaded C++ script").withStyle(ChatFormatting.WHITE))
            );
        } else {
            player.sendSystemMessage(
                Component.literal("[CppSqript] Find error in script " + errors.size() + " at:")
                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)
            );

            for (String err : errors ){
                player.sendSystemMessage(
                    Component.literal(" . " + err).withStyle(ChatFormatting.RED)
                );
            }
        }
    }

}
