package com.mnight.cppsqript.recipe;

import java.util.List;
import java.util.Set;

import com.mnight.cppsqript.parser.ScriptErrorTracker;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
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

        if (event.getPlayer() != null) {
            ScriptErrorTracker.broadcastToPlayer(event.getPlayer());
        } else {
            for (ServerPlayer player : event.getPlayerList().getPlayers()){
                ScriptErrorTracker.broadcastToPlayer(player);
            }
        }
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

        // ใช้ Map เพื่อรับประกันว่าไม่มี Recipe ID ไหนซ้ำกัน 100% (ป้องกันบั๊กจาก NeoForge เรียกอีเวนต์ซ้ำ)
        java.util.Map<ResourceLocation, RecipeHolder<?>> uniqueRecipes = new java.util.HashMap<>();

        // 1. นำ Recipe เดิมทั้งหมดของเกมใส่ลงไปใน Map (ยกเว้นตัวที่เราสั่งลบ)
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            if (!toRemove.contains(holder.id())) {
                uniqueRecipes.put(holder.id(), holder);
            }
        }

        // 2. นำ Recipe ใหม่ของเราใส่เข้าไป (ถ้าบังเอิญมี ID ซ้ำ มันจะเขียนทับตัวเก่าให้ทันที ปลอดภัยมาก)
        for (RecipeHolder<?> holder : toAdd) {
            uniqueRecipes.put(holder.id(), holder);
        }

        // 3. แทนที่ Recipe Set ทั้งหมดลงใน RecipeManager โดยดึงเฉพาะ Value กลับมา
        recipeManager.replaceRecipes(uniqueRecipes.values());
        
        System.out.println("[CppSqript] Successfully injected " + toAdd.size() + " recipes without ID conflicts!");
    }

}
