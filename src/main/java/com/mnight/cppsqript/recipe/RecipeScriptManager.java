package com.mnight.cppsqript.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mnight.cppsqript.parser.ScriptErrorTracker;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.*;

import java.util.*;

public class RecipeScriptManager {
    private static final Set<ResourceLocation> recipesToRemove = new HashSet<>();
    private static final List<RecipeHolder<?>> recipesToAdd = new ArrayList<>();

    public static void clear() {
        recipesToRemove.clear();
        recipesToAdd.clear();
    }

    public static Set<ResourceLocation> getRecipesToRemove() {
        return Collections.unmodifiableSet(recipesToRemove);
    }

    public static List<RecipeHolder<?>> getRecipesToAdd() {
        return Collections.unmodifiableList(recipesToAdd);
    }

    // =========================================================================
    // 1. Validation & Ingredient Helpers
    // =========================================================================

    /**
     * Validates that an Item ID exists in the game registry and is not minecraft:air.
     * Reports a warning and returns null if invalid.
     */
    public static Item validateAndGetItem(String itemId, String context) {
        try {
            ResourceLocation loc = ResourceLocation.parse(itemId);
            if (!BuiltInRegistries.ITEM.containsKey(loc)) {
                ScriptErrorTracker.reportWarning(context, "Unknown item ID '" + itemId + "' (item does not exist in registry).");
                return null;
            }
            Item item = BuiltInRegistries.ITEM.get(loc);
            if (item == Items.AIR) {
                ScriptErrorTracker.reportWarning(context, "Item '" + itemId + "' resolved to minecraft:air.");
                return null;
            }
            return item;
        } catch (Exception e) {
            ScriptErrorTracker.reportWarning(context, "Invalid ResourceLocation format '" + itemId + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses an input string into an Ingredient, supporting both standard Item IDs ("minecraft:dirt")
     * and Item Tags ("#minecraft:planks"). Returns null if the item ID is invalid.
     */
    private static Ingredient parseIngredient(String input, String context) {
        if (input == null || input.isBlank()) {
            ScriptErrorTracker.reportWarning(context, "Empty ingredient string provided.");
            return null;
        }

        if (input.startsWith("#")) {
            try {
                String tagName = input.substring(1);
                ResourceLocation tagLoc = ResourceLocation.parse(tagName);
                TagKey<Item> itemTag = TagKey.create(Registries.ITEM, tagLoc);
                return Ingredient.of(itemTag);
            } catch (Exception e) {
                ScriptErrorTracker.reportWarning(context, "Invalid tag format '" + input + "': " + e.getMessage());
                return null;
            }
        } else {
            Item item = validateAndGetItem(input, context);
            if (item == null) {
                return null;
            }
            return Ingredient.of(item);
        }
    }

    // =========================================================================
    // 2. Crafting Table & Removal Commands
    // =========================================================================

    public static void removeRecipe(String recipeId) {
        try {
            recipesToRemove.add(ResourceLocation.parse(recipeId));
        } catch (Exception e) {
            ScriptErrorTracker.reportWarning("remove", "Invalid recipe ID format '" + recipeId + "': " + e.getMessage());
        }
    }

    public static void addShapelessRecipe(String resultItemId, int resultCount, List<String> inputItemIds) {
        Item resultItem = validateAndGetItem(resultItemId, "shapeless");
        if (resultItem == null) return;

        NonNullList<Ingredient> ingredients = NonNullList.create();
        for (String inputId : inputItemIds) {
            Ingredient ing = parseIngredient(inputId, "shapeless");
            if (ing == null) return; // Abort recipe creation if any ingredient is invalid
            ingredients.add(ing);
        }

        ItemStack resultStack = new ItemStack(resultItem, resultCount);

        String idBase = resultItemId.replace(":", "_");
        String inputsHash = Integer.toHexString(Math.abs(inputItemIds.hashCode()));
        ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath("cppsqript", "shapeless_" + idBase + "_" + inputsHash);

        boolean alreadyExists = recipesToAdd.stream()
                .anyMatch(holder -> holder.id().equals(newRecipeId));

        if (!alreadyExists) {
            ShapelessRecipe recipe = new ShapelessRecipe(
                    "",
                    CraftingBookCategory.MISC,
                    resultStack,
                    ingredients
            );
            recipesToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
        }
    }

    public static void addShapedRecipe(String resultItemId, int resultCount, List<String> patternRows, Map<Character, String> keyMap) {
        Item resultItem = validateAndGetItem(resultItemId, "shaped");
        if (resultItem == null) return;

        Map<Character, Ingredient> ingredientMap = new HashMap<>();
        for (Map.Entry<Character, String> entry : keyMap.entrySet()) {
            Ingredient ing = parseIngredient(entry.getValue(), "shaped");
            if (ing == null) return; // Abort recipe creation if any key ingredient is invalid
            ingredientMap.put(entry.getKey(), ing);
        }

        ItemStack resultStack = new ItemStack(resultItem, resultCount);
        ShapedRecipePattern pattern = ShapedRecipePattern.of(ingredientMap, patternRows);

        String idBase = resultItemId.replace(":", "_");
        int combinedHash = Math.abs(Objects.hash(patternRows, keyMap));
        String recipeHash = Integer.toHexString(combinedHash);
        ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath("cppsqript", "shaped_" + idBase + "_" + recipeHash);

        boolean alreadyExists = recipesToAdd.stream()
                .anyMatch(holder -> holder.id().equals(newRecipeId));

        if (!alreadyExists) {
            ShapedRecipe recipe = new ShapedRecipe(
                    "",
                    CraftingBookCategory.MISC,
                    pattern,
                    resultStack
            );
            recipesToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
        }
    }

    // =========================================================================
    // 3. Furnace, Smoker, Campfire & Stonecutter Recipes
    // =========================================================================

    private static <T extends AbstractCookingRecipe> void addCookingRecipe(
            String typePrefix,
            String resultItemId,
            String inputItemId,
            float experience,
            int cookingTime,
            AbstractCookingRecipe.Factory<T> factory
    ) {
        Item resultItem = validateAndGetItem(resultItemId, typePrefix);
        if (resultItem == null) return;

        Ingredient ingredient = parseIngredient(inputItemId, typePrefix);
        if (ingredient == null) return;

        ItemStack resultStack = new ItemStack(resultItem, 1);

        String idBase = resultItemId.replace(":", "_");
        int combinedHash = Math.abs(Objects.hash(typePrefix, resultItemId, inputItemId, cookingTime));
        ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath(
                "cppsqript", typePrefix + "_" + idBase + "_" + Integer.toHexString(combinedHash)
        );

        boolean alreadyExists = recipesToAdd.stream().anyMatch(h -> h.id().equals(newRecipeId));
        if (!alreadyExists) {
            T recipe = factory.create("", CookingBookCategory.MISC, ingredient, resultStack, experience, cookingTime);
            recipesToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
        }
    }

    public static void addSmelting(String resultId, String inputId, float xp, int time) {
        addCookingRecipe("smelting", resultId, inputId, xp, time, SmeltingRecipe::new);
    }

    public static void addBlasting(String resultId, String inputId, float xp, int time) {
        addCookingRecipe("blasting", resultId, inputId, xp, time, BlastingRecipe::new);
    }

    public static void addSmoking(String resultId, String inputId, float xp, int time) {
        addCookingRecipe("smoking", resultId, inputId, xp, time, SmokingRecipe::new);
    }

    public static void addCampfire(String resultId, String inputId, float xp, int time) {
        addCookingRecipe("campfire", resultId, inputId, xp, time, CampfireCookingRecipe::new);
    }

    public static void addStonecutting(String resultItemId, int resultCount, String inputItemId) {
        Item resultItem = validateAndGetItem(resultItemId, "stonecutting");
        if (resultItem == null) return;

        Ingredient ingredient = parseIngredient(inputItemId, "stonecutting");
        if (ingredient == null) return;

        ItemStack resultStack = new ItemStack(resultItem, resultCount);

        String idBase = resultItemId.replace(":", "_");
        int combinedHash = Math.abs(Objects.hash("stonecutting", resultItemId, resultCount, inputItemId));
        ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath(
                "cppsqript", "stonecutting_" + idBase + "_" + Integer.toHexString(combinedHash)
        );

        boolean alreadyExists = recipesToAdd.stream().anyMatch(h -> h.id().equals(newRecipeId));
        if (!alreadyExists) {
            StonecutterRecipe recipe = new StonecutterRecipe("", ingredient, resultStack);
            recipesToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
        }
    }

    // =========================================================================
    // 4. Custom Raw JSON Recipe (Supports Vanilla & Modded Machines)
    // =========================================================================

    public static void addCustomJson(String customIdSuffix, String rawJson) {
        try {
            JsonElement jsonElement = JsonParser.parseString(rawJson);

            Recipe<?> recipe = Recipe.CODEC.parse(JsonOps.INSTANCE, jsonElement)
                    .getOrThrow(errorMsg -> new RuntimeException("JSON Recipe Decode Error: " + errorMsg));

            String cleanSuffix = customIdSuffix.replaceAll("[^a-zA-Z0-9_]", "_").toLowerCase();
            int jsonHash = Math.abs(rawJson.hashCode());
            ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath(
                    "cppsqript", "custom_" + cleanSuffix + "_" + Integer.toHexString(jsonHash)
            );

            boolean alreadyExists = recipesToAdd.stream().anyMatch(h -> h.id().equals(newRecipeId));
            if (!alreadyExists) {
                recipesToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse custom JSON recipe '" + customIdSuffix + "': " + e.getMessage());
        }
    }
}