package com.mnight.cppsqript.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class RecipeScriptManager {
	private static final Set<ResourceLocation> recipeToRemove = new HashSet<>();
	private static final List<RecipeHolder<?>> recipeToAdd = new ArrayList<>();

	// Clear สตรีมข้อมูลสคริปต์เก่าก่อน Reload
	public static void clear(){
		recipeToRemove.clear();
		recipeToAdd.clear();
	}

	// API สำหรับลบ Recipe ตาม ResourceLocation ID (เช่น "minecraft:diamond_sword")
	public static void removeRecipe(String recipeId){
		recipeToRemove.add(ResourceLocation.parse(recipeId));
	}

	// API สำหรับเพิ่ม Shapeless Recipe
	public static void addShapelessRecipe(String resultItemId, int resultCount, List<String> inputItemIds){
		ResourceLocation resultLoc = ResourceLocation.parse(resultItemId);
		Item resultItem = BuiltInRegistries.ITEM.get(resultLoc);
		ItemStack resultStack = new ItemStack(resultItem, resultCount);

		NonNullList<Ingredient> ingredients = NonNullList.create();
		for (String inputId : inputItemIds){
			Item inpuItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(inputId));
			ingredients.add(Ingredient.of(inpuItem));
		}


		String idBase = resultItemId.replace(":","_");
		String inputsHash = Integer.toHexString(Math.abs(inputItemIds.hashCode()));
		ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath("cppsqript", "shapeless_" + idBase + "_" + inputsHash);

		boolean alreadyExisits = recipeToAdd.stream()
			.anyMatch(holder -> holder.id().equals(newRecipeId));
		
		if (!alreadyExisits) {
			ShapelessRecipe recipe =new ShapelessRecipe(
				"", 
                    CraftingBookCategory.MISC, 
                    resultStack, 
                    ingredients);
			recipeToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
		}

		ShapelessRecipe recipe = new ShapelessRecipe(
			"",
			CraftingBookCategory.MISC,
			resultStack,
			ingredients
		);

		recipeToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
	}

	public static void addShapedRecipe(String resultItemId, int resultCount, List<String> patternRows, Map<Character, String> keyMap){
		ResourceLocation resultLoc = ResourceLocation.parse(resultItemId);
		Item resultItem = BuiltInRegistries.ITEM.get(resultLoc);
		ItemStack resultStack = new ItemStack(resultItem, resultCount);

		Map<Character, Ingredient> ingredientMap = new HashMap<>();
		for (Map.Entry<Character, String> entry : keyMap.entrySet()){
			Item ingredientItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(entry.getValue()));
			ingredientMap.put(entry.getKey(), Ingredient.of(ingredientItem));
		}

		ShapedRecipePattern pattern = ShapedRecipePattern.of(ingredientMap, patternRows);

		String idBase = resultItemId.replace(":","_");
		int combinedHash = Math.abs(Objects.hash(patternRows, keyMap));
		String recipeHash = Integer.toHexString(combinedHash);
		ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath("cppsqript", "shaped_" + idBase + "_" + recipeHash);

		boolean alreadyExisits = recipeToAdd.stream()
			.anyMatch(holder -> holder.id().equals(newRecipeId));

		if (!alreadyExisits) {
			ShapedRecipe recipe = new ShapedRecipe(
				"",
				CraftingBookCategory.MISC,
				pattern,
				resultStack
			);
			recipeToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
		}
	}

	// Getter สภาพแวดล้อมข้อมูล
	public static Set<ResourceLocation> getRecipesToRemove() {
		return recipeToRemove;
	}

	public static List<RecipeHolder<?>> getRecipetoAdd(){
		return recipeToAdd;
	}
}
