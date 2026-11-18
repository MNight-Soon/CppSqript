package com.mnight.cppsqript.api;

import java.util.*;

public class CppSqriptAPI {
    private static final Map<String, ScriptCommandHandler> COMMANDS = new LinkedHashMap<>();
    private static final List<String> HEADER_SIGNATURES = new ArrayList<>();

    /**
     * ลงทะเบียนคำสั่งใหม่เข้าสู่ CppSqript Framework
     * @param commandName ชื่อคำสั่งที่จะใช้ในไฟล์ .cpp (เช่น "crushing")
     * @param cppSignature รูปแบบฟังก์ชันสำหรับเจนลงไฟล์ cppsqript.hpp (เช่น "void crushing(const char* output, const char* input)")
     * @param handler โค้ด Java ที่จะทำงานเมื่อคำสั่งนี้ถูกเรียก
     */

    public static void registerCommand(String commandName, String cppSignature, ScriptCommandHandler handler) {
        if (COMMANDS.containsKey(commandName)) {
            System.out.println("[CppSqript] Command '" + commandName + "' is already registered. Skipping duplicate.");
            return;
        }
        COMMANDS.put(commandName, handler);
        if (cppSignature != null && !cppSignature.isBlank() && !HEADER_SIGNATURES.contains(cppSignature)) {
            HEADER_SIGNATURES.add(cppSignature);
        }
    }

    public static ScriptCommandHandler getCommand(String commandName) {
        return COMMANDS.get(commandName);
    }

    public static List<String> getHeaderSignatures() {
        return Collections.unmodifiableList(HEADER_SIGNATURES);
    }
}
