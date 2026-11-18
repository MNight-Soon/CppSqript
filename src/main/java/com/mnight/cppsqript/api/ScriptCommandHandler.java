package com.mnight.cppsqript.api;

import java.util.List;

@FunctionalInterface 
public interface ScriptCommandHandler {
    /**
     * @param args รายการ Argument ที่ถูกแปลงค่ามาจากสคริปต์ C++ แล้ว (String, Integer, Float, List, Map.Entry)
     */
    void execute(List<Object> args) throws Exception;
}
