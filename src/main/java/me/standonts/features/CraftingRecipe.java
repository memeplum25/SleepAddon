package me.standonts.features;

import net.minecraft.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * A recipe of the vanilla recipe list, reduced to a grid layout that can be replayed on a
 * crafting grid of any width.
 */
public final class CraftingRecipe {

    /** Number of columns of the recipe grid, 1 to 3 */
    public final int width;
    /** Number of rows of the recipe grid, 1 to 3 */
    public final int height;
    /** Ingredients in row major order, null means an empty grid slot */
    public final ItemStack[] items;
    public final ItemStack output;
    /** How many of each ingredient the recipe needs */
    public final Map<ItemStack, Integer> ingredientCounts = new HashMap<>(9);

    public CraftingRecipe(int width, int height, ItemStack[] items, ItemStack output) {
        this.width = width;
        this.height = height;
        this.items = items;
        this.output = output;
        for (ItemStack ingredient : items) {
            addIngredient(ingredient);
        }
    }

    private void addIngredient(ItemStack ingredient) {
        if (ingredient == null) {
            return;
        }
        for (ItemStack known : this.ingredientCounts.keySet()) {
            if (known.isItemEqual(ingredient)) {
                this.ingredientCounts.put(known, this.ingredientCounts.get(known) + 1);
                return;
            }
        }
        this.ingredientCounts.put(ingredient, 1);
    }

}