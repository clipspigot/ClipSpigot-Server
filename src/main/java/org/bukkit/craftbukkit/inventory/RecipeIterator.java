package org.bukkit.craftbukkit.inventory;

import java.util.Iterator;

import net.minecraft.server.CraftingManager;
import net.minecraft.server.IRecipe;
import net.minecraft.server.RecipesFurnace;

import org.bukkit.inventory.Recipe;

public class RecipeIterator implements Iterator<Recipe> {
	private final Iterator<IRecipe> recipes;
	private final Iterator<net.minecraft.server.ItemStack> smeltingCustom;
	private final Iterator<net.minecraft.server.ItemStack> smeltingVanilla;
	private Iterator<?> removeFrom = null;

	public RecipeIterator() {
		recipes = CraftingManager.getInstance().getRecipes().iterator();
		smeltingCustom = RecipesFurnace.getInstance().customRecipes.keySet().iterator();
		smeltingVanilla = RecipesFurnace.getInstance().recipes.keySet().iterator();
	}

	@Override
	public boolean hasNext() {
		return recipes.hasNext() || smeltingCustom.hasNext() || smeltingVanilla.hasNext();
	}

	@Override
	public Recipe next() {
		if (recipes.hasNext()) {
			removeFrom = recipes;
			return recipes.next().toBukkitRecipe();
		} else {
			net.minecraft.server.ItemStack item;
			if (smeltingCustom.hasNext()) {
				removeFrom = smeltingCustom;
				item = smeltingCustom.next();
			} else {
				removeFrom = smeltingVanilla;
				item = smeltingVanilla.next();
			}

			CraftItemStack stack = CraftItemStack.asCraftMirror(RecipesFurnace.getInstance().getResult(item));

			return new CraftFurnaceRecipe(stack, CraftItemStack.asCraftMirror(item));
		}
	}

	@Override
	public void remove() {
		if (removeFrom == null)
			throw new IllegalStateException();
		removeFrom.remove();
	}
}
