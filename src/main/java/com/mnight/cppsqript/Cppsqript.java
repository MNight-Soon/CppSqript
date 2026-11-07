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
    public void onAddReloadListeners(AddReloadListenerEvent event){
        loadCppScripts();
    }

    private void loadCppScripts(){

        //testing
        RecipeScriptManager.clear();

        RecipeScriptManager.removeRecipe("minecraft:diamond_sword");

        RecipeScriptManager.addShapelessRecipe("minecraft:diamond", 1, List.of("minecraft:dirt", "minecraft:dirt"));

        System.out.println("[CppSqript] Loaded recipes queue successfully!");
    }
}
