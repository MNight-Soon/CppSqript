package com.mnight.cppsqript.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesUpdatedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;

@EventBusSubscriber (modid = "cppsqript")
public class RecipeInjector {
    /**
     * ดักจับ Event ตอน Datapack Sync มายังผู้เล่น (Server Side / Singleplayer)
     */
    @SubscribeEvent 
    public static void onDatapackSync(OnDatapackSyncEvent event){
        RecipeManager recipeManager = event.getPlayerList().getServer().getRecipeManager();
        injectRecipes(recipeManager);
    }

    /**
     * ดักจับ Event ตอน Client ได้รับข้อมูล Recipe อัปเดตเรียบร้อย (Client Side GUI / Recipe Book)
     */
    @SubscribeEvent 
    public static void onRecipeUpdated(RecipesUpdatedEvent event){
        injectRecipes(event.getRecipeManager());
    }

    private static void injectRecipes(RecipeManager recipeManager){
        Set<ResourceLocation> toRemove = RecipeScriptManager.getRecipesToRemove();
        List<RecipeHolder<?>> toAdd = RecipeScriptManager.getRecipetoAdd();

        if(toRemove.isEmpty() && toAdd.isEmpty()){
            return;
        }

        // ดึง Recipe ทั้งหมดในปัจจุบันออกมาปรับแต่ง
        List<RecipeHolder<?>> currentRecipes = new ArrayList<>(recipeManager.getRecipes());

        // 1. ดำเนินการลบ Recipe ตาม ResourceLocation ID
        if(!toRemove.isEmpty()){
            currentRecipes.removeIf(holder -> toRemove.contains(holder.id()));
        }

        // 2. เพิ่ม Recipe ใหม่จากสคริปต์
        if (!toAdd.isEmpty()) {
            currentRecipes.addAll(toAdd);
        }

        // 3. แทนที่ Recipe Set ทั้งหมดลงใน RecipeManager
        recipeManager.replaceRecipes(currentRecipes);
        System.out.println("[CppSqript] Successfully injected/removed dynamic recipes!");
    }

}
