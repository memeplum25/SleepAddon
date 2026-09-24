package me.standonts.features;

import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.oredict.ShapedOreRecipe;
import net.minecraftforge.oredict.ShapelessOreRecipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Index of the vanilla recipes, used to tell whether the player is carrying the ingredients
 * of an item and how those ingredients have to be spread over a crafting grid.
 */
public final class CraftingRecipes {

    /** Damage value used by ore dictionary recipes to accept every item of the family */
    private static final int ANY_DAMAGE = 32767;
    private static final int SHAPELESS_GRID_WIDTH = 4;

    private static List<CraftingRecipe> recipes;

    private CraftingRecipes() {}

    /**
     * First recipe producing the item that the player can craft on a grid of that width,
     * or null when the player cannot craft the item.
     */
    public static CraftingRecipe findCraftable(Item target, int gridWidth) {
        for (CraftingRecipe recipe : all()) {
            if (recipe.output.getItem() == target
                    && recipe.width <= gridWidth
                    && recipe.height <= gridWidth
                    && craftableAmount(recipe) > 0) {
                return recipe;
            }
        }
        return null;
    }

    /** True when the carried stack fulfils the ingredient, ore dictionary recipes accept every damage value */
    public static boolean matches(ItemStack ingredient, ItemStack carried) {
        return carried.getItem() == ingredient.getItem()
                && (carried.getItemDamage() == ingredient.getItemDamage() || ingredient.getItemDamage() == ANY_DAMAGE);
    }

    /** How many times the player could craft this recipe with what they are carrying */
    private static int craftableAmount(CraftingRecipe recipe) {
        int amount = Integer.MAX_VALUE;
        for (Map.Entry<ItemStack, Integer> ingredient : recipe.ingredientCounts.entrySet()) {
            amount = Math.min(amount, countCarried(ingredient.getKey()) / ingredient.getValue());
        }
        return amount == Integer.MAX_VALUE ? 0 : amount;
    }

    private static int countCarried(ItemStack ingredient) {
        final Minecraft mc = Minecraft.getMinecraft();
        int count = 0;
        if (mc.thePlayer != null) {
            for (ItemStack carried : mc.thePlayer.inventory.mainInventory) {
                if (carried != null && matches(ingredient, carried)) {
                    count += carried.stackSize;
                }
            }
        }
        return count;
    }

    private static List<CraftingRecipe> all() {
        if (recipes == null) {
            recipes = Collections.unmodifiableList(loadRecipes());
        }
        return recipes;
    }

    /** Clears the cached recipe index when a client reloads its recipe registry. */
    public static void invalidate() {
        recipes = null;
    }

    private static List<CraftingRecipe> loadRecipes() {
        final List<CraftingRecipe> loaded = new ArrayList<>();
        for (IRecipe recipe : CraftingManager.getInstance().getRecipeList()) {
            try {
                final CraftingRecipe converted = convert(recipe);
                if (converted != null && converted.output != null) {
                    loaded.add(converted);
                }
            } catch (RuntimeException ignored) {
            }
        }
        return loaded;
    }

    private static CraftingRecipe convert(IRecipe recipe) {
        if (recipe instanceof ShapedRecipes) {
            final ShapedRecipes shaped = (ShapedRecipes) recipe;
            return new CraftingRecipe(shaped.recipeWidth, shaped.recipeHeight,
                    shaped.recipeItems, shaped.getRecipeOutput());
        }
        if (recipe instanceof ShapelessRecipes) {
            final ShapelessRecipes shapeless = (ShapelessRecipes) recipe;
            final ItemStack[] items = shapeless.recipeItems.toArray(new ItemStack[0]);
            return new CraftingRecipe(gridWidthOf(items.length), gridWidthOf(items.length),
                    items, shapeless.getRecipeOutput());
        }
        if (recipe instanceof ShapedOreRecipe) {
            final ShapedOreRecipe shaped = (ShapedOreRecipe) recipe;
            final ItemStack[] items = resolveIngredients(shaped.getInput());
            if (items == null) {
                return null;
            }
            final int width = ObfuscationReflectionHelper.getPrivateValue(ShapedOreRecipe.class, shaped, "width");
            final int height = ObfuscationReflectionHelper.getPrivateValue(ShapedOreRecipe.class, shaped, "height");
            return new CraftingRecipe(width, height, items, shaped.getRecipeOutput());
        }
        if (recipe instanceof ShapelessOreRecipe) {
            final ShapelessOreRecipe shapeless = (ShapelessOreRecipe) recipe;
            final ItemStack[] items = resolveIngredients(shapeless.getInput().toArray());
            if (items == null) {
                return null;
            }
            return new CraftingRecipe(gridWidthOf(items.length), gridWidthOf(items.length),
                    items, shapeless.getRecipeOutput());
        }
        return null;
    }

    /**
     * Turns the ingredients of an ore dictionary recipe into plain stacks, returns null when
     * one of them is a choice between several items because such a recipe cannot be replayed.
     */
    private static ItemStack[] resolveIngredients(Object[] ingredients) {
        final ItemStack[] items = new ItemStack[ingredients.length];
        for (int i = 0; i < ingredients.length; i++) {
            final Object ingredient = ingredients[i];
            if (ingredient == null) {
                continue;
            }
            if (ingredient instanceof ItemStack) {
                items[i] = (ItemStack) ingredient;
            } else if (ingredient instanceof List && isSingleStack((List<?>) ingredient)) {
                items[i] = (ItemStack) ((List<?>) ingredient).get(0);
            } else {
                return null;
            }
        }
        return items;
    }

    private static boolean isSingleStack(List<?> options) {
        return options.size() == 1 && options.get(0) instanceof ItemStack;
    }

    private static int gridWidthOf(int ingredientCount) {
        return ingredientCount > SHAPELESS_GRID_WIDTH ? 3 : 2;
    }

}
