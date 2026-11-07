package com.mnight.cppsqript.recipe;

import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

		ResourceLocation newRecipeId = ResourceLocation.fromNamespaceAndPath("cppsqript", "shapeless_" + System.currentTimeMillis() + "_" + Math.random());
		ShapelessRecipe recipe = new ShapelessRecipe(
			"",
			CraftingBookCategory.MISC,
			resultStack,
			ingredients
		);

		recipeToAdd.add(new RecipeHolder<>(newRecipeId, recipe));
	}

	// Getter สภาพแวดล้อมข้อมูล
	public static Set<ResourceLocation> getRecipesToRemove() {
		return recipeToRemove;
	}

	public static List<RecipeHolder<?>> getRecipetoAdd(){
		return recipeToAdd;
	}
}
