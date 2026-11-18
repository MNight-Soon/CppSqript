package com.mnight.cppsqript.api;

import net.neoforged.bus.api.Event;

public class RegisterCppSqriptCommandsEvent extends Event{
    public void register(String commandName, String cppSignature, ScriptCommandHandler handler) {
        CppSqriptAPI.registerCommand(commandName, cppSignature, handler);
    }
}
