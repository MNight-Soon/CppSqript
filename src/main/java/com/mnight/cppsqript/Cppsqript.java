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

    private void loadCppScripts() {
        RecipeScriptManager.clear();

        File scriptDir = new File("config/cppsqript");
        if (!scriptDir.exists()) {
            scriptDir.mkdirs();
            createDefaultHeader(scriptDir);
            createDefaultScript(scriptDir);
        }
        // โหลดไฟล์สคริปต์มาประมวลผล
        int count = CppScriptLoader.loadAllScripts(scriptDir.toPath());
        System.out.println("[CppSqript] Finished loading " + count + " C++ script file(s).");

    }
    
    private void createDefaultHeader(File scriptDir){
        File headerFile = new File(scriptDir, "cppsqript.hpp");
        try {
            String headerCode = """
                    #pragma once
                    #include <initializer_list>
                    #include <utility>

                    namespace ServerEvents {
                        class Recipes {
                        public:
                            void remove(const char* recipeId) {}
                            void shapeless(const char* result, int count, std::initializer_list<const char*> ingredients) {}
                            void shaped(const char* result, int count, std::initializer_list<const char*> pattern, std::initializer_list<std::pair<char, const char*>> keys) {}
                        };
                    }

                    // รองรับการเรียกใช้งานแบบ Global นอกฟังก์ชัน
                    inline void remove(const char* recipeId) {}
                    inline void shapeless(const char* result, int count, std::initializer_list<const char*> ingredients) {}
                    inline void shaped(const char* result, int count, std::initializer_list<const char*> pattern, std::initializer_list<std::pair<char, const char*>> keys) {}
                    """;
            Files.writeString(headerFile.toPath(), headerCode);
            System.out.println("[CppSqript] Created default header: cppsqript.hpp");
        } catch (IOException e){
            System.err.println("[CppSqript] Failed to create cppsqript.hpp");
            e.printStackTrace();
        }
    }

    private void createDefaultScript(File scriptDir) {
        File defaultScript = new File(scriptDir, "recipes.cpp");
        try {
            String sampleCode = """
                    #include "cppsqript.hpp"

                    void register_recipes(ServerEvents::Recipes& event) {
                        // ตัวอย่างการใช้งานตัวแปร (Variables)
                        // auto planks = "#minecraft:planks";
                        // const char* stick = "minecraft:stick";

                        // 1. ลบสูตรคราฟต์เดิม
                        // event.remove("minecraft:diamond_sword");

                        // 2. เพิ่มสูตรคราฟต์แบบไม่สนตำแหน่ง (Shapeless)
                        // event.shapeless("minecraft:diamond", 1, {"minecraft:dirt", "minecraft:dirt"});

                        // 3. เพิ่มสูตรคราฟต์แบบจัดวางตำแหน่ง 3x3 (Shaped)
                        // event.shaped("minecraft:diamond_sword", 1, {
                        //     " P ",
                        //     " P ",
                        //     " S "
                        // }, {
                        //     {'P', planks},
                        //     {'S', stick}
                        // });
                    }
                    """;
            Files.writeString(defaultScript.toPath(), sampleCode);
            System.out.println("[CppSqript] Created default script: recipes.cpp");
        } catch (IOException e) {
            System.err.println("[CppSqript] Failed to create default recipes.cpp");
            e.printStackTrace();
        }
    }
}
