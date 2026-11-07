package com.mnight.cppsqript;

import com.mnight.cppsqript.recipe.RecipeScriptManager;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.List;

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
        loadMockCppScripts();
    }

    private void loadMockCppScripts() {
        RecipeScriptManager.clear();
        
        // ทดสอบ Mock สคริปต์ C++:
        // 1. ลบ Recipe ดาบเพชร (minecraft:diamond_sword)
        RecipeScriptManager.removeRecipe("minecraft:diamond_sword");
        
        // 2. คราฟต์ Diamond จาก Dirt 2 ก้อน
        RecipeScriptManager.addShapelessRecipe("minecraft:diamond", 1, List.of("minecraft:dirt"));
    }
}
