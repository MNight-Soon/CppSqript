package com.mnight.cppsqript;

import com.mnight.cppsqript.parser.CppScriptLoader;
import com.mnight.cppsqript.recipe.RecipeScriptManager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Cppsqript.MOD_ID)
public class Cppsqript {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "cppsqript";

    public Cppsqript(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        loadCppScripts();
    }

    /*private void loadMockCppScripts() {
        RecipeScriptManager.clear();
        
        // ทดสอบ Mock สคริปต์ C++:
        // 1. ลบ Recipe ดาบเพชร (minecraft:diamond_sword)
        RecipeScriptManager.removeRecipe("minecraft:diamond_sword");
        
        // 2. คราฟต์ Diamond จาก Dirt 2 ก้อน
        RecipeScriptManager.addShapelessRecipe("minecraft:diamond", 1, List.of("minecraft:dirt"));
    }*/

    private void loadCppScripts() {
        RecipeScriptManager.clear();

        File scriptDir = new File("config/cppsqript");
        if (!scriptDir.exists()) {
            scriptDir.mkdirs();
            File testScript = new File(scriptDir, "recipes.cpp");
            try {
                String defaultScript = """
                        // ตัวอย่างสคริปต์ CppSqript
                        // remove("minecraft:diamond_sword");
                        // shapeless("minecraft:diamond", 1, {"minecraft:dirt", "minecraft:dirt"});
                        """;
                Files.writeString(testScript.toPath(), defaultScript);
                System.out.println("[CppSqript] Created default recipes.cpp at " + testScript.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("[CppSqript] Failed to create default recipes.cpp");
                e.printStackTrace();
            }
        }

        // โหลดไฟล์สคริปต์มาประมวลผล
        int count = CppScriptLoader.loadAllScripts(scriptDir.toPath());
        System.out.println("[CppSqript] Finished loading " + count + " C++ script file(s).");

    }
}
