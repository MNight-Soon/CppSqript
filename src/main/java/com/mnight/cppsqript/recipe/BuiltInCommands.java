package com.mnight.cppsqript.recipe;

import com.mnight.cppsqript.api.CppSqriptAPI;
import java.util.*;

public class BuiltInCommands {
    @SuppressWarnings("unchecked")
    public static void registerAll() {
        // 1. คำสั่ง remove
        CppSqriptAPI.registerCommand(
            "remove",
            "void remove(const char* recipeId)",
            args -> {
                String recipeId = String.valueOf(args.get(0));
                RecipeScriptManager.removeRecipe(recipeId);
            }
        );

        // 2. คำสั่ง shapeless
        CppSqriptAPI.registerCommand(
            "shapeless",
            "void shapeless(const char* result, int count, std::initializer_list<const char*> ingredients)",
            args -> {
                String resultId = String.valueOf(args.get(0));
                int count = ((Number) args.get(1)).intValue();
                List<String> inputs = ((List<Object>) args.get(2)).stream().map(String::valueOf).toList();
                RecipeScriptManager.addShapelessRecipe(resultId, count, inputs);
            }
        );

        // 3. คำสั่ง shaped
        CppSqriptAPI.registerCommand(
            "shaped",
            "void shaped(const char* result, int count, std::initializer_list<const char*> pattern, std::initializer_list<std::pair<char, const char*>> keys)",
            args -> {
                String resultId = String.valueOf(args.get(0));
                int count = ((Number) args.get(1)).intValue();
                List<String> pattern = ((List<Object>) args.get(2)).stream().map(String::valueOf).toList();
                
                Map<Character, String> keyMap = new HashMap<>();
                for (Object item : (List<Object>) args.get(3)) {
                    if (item instanceof Map.Entry<?, ?> pair) {
                        keyMap.put((Character) pair.getKey(), String.valueOf(pair.getValue()));
                    } else if (item instanceof List<?> list && list.size() == 2 && list.get(0) instanceof Character ch) {
                        keyMap.put(ch, String.valueOf(list.get(1)));
                    } else {
                        throw new IllegalArgumentException("Expected key-value pair like {'D', \"minecraft:diamond\"}");
                    }
                }
                RecipeScriptManager.addShapedRecipe(resultId, count, pattern, keyMap);
            }
        );

        // 4. Smelting (เวลาเผามาตรฐานคือ 200 ticks = 10 วินาที)
        CppSqriptAPI.registerCommand(
            "smelting",
            "void smelting(const char* result, const char* input, float xp = 0.1f, int cookingTime = 200)",
            args -> {
                String result = String.valueOf(args.get(0));
                String input = String.valueOf(args.get(1));
                float xp = args.size() > 2 ? ((Number) args.get(2)).floatValue() : 0.1f;
                int time = args.size() > 3 ? ((Number) args.get(3)).intValue() : 200;
                RecipeScriptManager.addSmelting(result, input, xp, time);
            }
        );

        // 5. Blasting (เวลาเผามาตรฐานคือ 100 ticks = 5 วินาที)
        CppSqriptAPI.registerCommand(
            "blasting",
            "void blasting(const char* result, const char* input, float xp = 0.1f, int cookingTime = 100)",
            args -> {
                String result = String.valueOf(args.get(0));
                String input = String.valueOf(args.get(1));
                float xp = args.size() > 2 ? ((Number) args.get(2)).floatValue() : 0.1f;
                int time = args.size() > 3 ? ((Number) args.get(3)).intValue() : 100;
                RecipeScriptManager.addBlasting(result, input, xp, time);
            }
        );

        // 6. Smoking (เวลาเผามาตรฐานคือ 100 ticks = 5 วินาที)
        CppSqriptAPI.registerCommand(
            "smoking",
            "void smoking(const char* result, const char* input, float xp = 0.1f, int cookingTime = 100)",
            args -> {
                String result = String.valueOf(args.get(0));
                String input = String.valueOf(args.get(1));
                float xp = args.size() > 2 ? ((Number) args.get(2)).floatValue() : 0.1f;
                int time = args.size() > 3 ? ((Number) args.get(3)).intValue() : 100;
                RecipeScriptManager.addSmoking(result, input, xp, time);
            }
        );

        // 7. Campfire Cooking (เวลาเผามาตรฐานคือ 600 ticks = 30 วินาที)
        CppSqriptAPI.registerCommand(
            "campfire",
            "void campfire(const char* result, const char* input, float xp = 0.1f, int cookingTime = 600)",
            args -> {
                String result = String.valueOf(args.get(0));
                String input = String.valueOf(args.get(1));
                float xp = args.size() > 2 ? ((Number) args.get(2)).floatValue() : 0.1f;
                int time = args.size() > 3 ? ((Number) args.get(3)).intValue() : 600;
                RecipeScriptManager.addCampfire(result, input, xp, time);
            }
        );

        // 8. Stonecutting
        CppSqriptAPI.registerCommand(
            "stonecutting",
            "void stonecutting(const char* result, int count, const char* input)",
            args -> {
                String result = String.valueOf(args.get(0));
                int count = ((Number) args.get(1)).intValue();
                String input = String.valueOf(args.get(2));
                RecipeScriptManager.addStonecutting(result, count, input);
            }
        );

        // 9. Custom JSON Recipe (รองรับทุกเครื่องจักรและทุก Mod)
        CppSqriptAPI.registerCommand(
            "custom",
            "void custom(const char* idName, const char* jsonRecipe)",
            args -> {
                String idName = String.valueOf(args.get(0));
                String json = String.valueOf(args.get(1));
                RecipeScriptManager.addCustomJson(idName, json);
            }
        );
    }
}