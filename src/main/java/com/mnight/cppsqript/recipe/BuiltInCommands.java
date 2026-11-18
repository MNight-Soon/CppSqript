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
    }
}