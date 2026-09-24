package me.standonts.features;

import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiCrafting;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Adds one click crafting buttons next to the crafting table GUI. The buttons are plain forge
 * buttons, so the vanilla GUI draws them and reports their clicks, the crafting itself replays
 * the slot clicks a player would make by hand.
 */
public final class AutoCraft {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final int BUTTON_SIZE = 20;
    private static final int BUTTON_GAP = 2;
    private static final int BUTTON_MARGIN = 105;
    private static final int ICON_OFFSET = 2;
    private static final int GRID_WIDTH = 3;
    private static final int GRID_SLOTS = GRID_WIDTH * GRID_WIDTH;
    private static final int FIRST_GRID_SLOT = 1;
    private static final int RESULT_SLOT = 0;
    private static final int CARRIED_SLOTS = 36;
    private static final long STATUS_INTERVAL_MILLIS = 250L;

    private static final List<CraftButton> BUTTONS = Arrays.asList(
            new CraftButton(0, Items.iron_helmet),
            new CraftButton(1, Items.iron_chestplate),
            new CraftButton(2, Items.iron_leggings),
            new CraftButton(3, Items.iron_boots),
            new CraftButton(4, Item.getItemFromBlock(Blocks.ladder)),
            new CraftButton(5, Items.stick),
            new CraftButton(6, Items.diamond_sword)
    );

    private static final boolean[] CRAFTABLE = new boolean[BUTTONS.size()];
    private static long statusCheckedAt;

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiCrafting) || !ExampleConfig.autoCraftEnabled) {
            return;
        }
        layoutButtons(event.gui.width, event.gui.height);
        event.buttonList.addAll(BUTTONS);
    }

    @SubscribeEvent
    public void onGuiClosed(GuiScreenEvent event) {
        if (event.gui instanceof GuiCrafting) {
            CraftingRecipes.invalidate();
        }
    }

    @SubscribeEvent
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event.button instanceof CraftButton) {
            craft(((CraftButton) event.button).targetItem);
        }
    }

    private static void layoutButtons(int screenWidth, int screenHeight) {
        final int totalHeight = BUTTONS.size() * BUTTON_SIZE + (BUTTONS.size() - 1) * BUTTON_GAP;
        final int x = screenWidth / 2 + BUTTON_MARGIN;
        final int y = (screenHeight - totalHeight) / 2;
        for (int i = 0; i < BUTTONS.size(); i++) {
            final CraftButton button = BUTTONS.get(i);
            button.xPosition = x;
            button.yPosition = y + i * (BUTTON_SIZE + BUTTON_GAP);
        }
    }

    private static void craft(Item target) {
        if (MC.thePlayer == null || MC.playerController == null) {
            return;
        }
        final Container container = MC.thePlayer.openContainer;
        if (MC.thePlayer.inventory.getItemStack() != null
                || container.inventorySlots.size() < FIRST_GRID_SLOT + GRID_SLOTS + CARRIED_SLOTS
                || !isGridEmpty(container)) {
            return;
        }
        final CraftingRecipe recipe = CraftingRecipes.findCraftable(target, GRID_WIDTH);
        if (recipe == null || recipe.output.getItem() != target) {
            return;
        }
        if (fillGrid(container, recipe)) {
            slotClick(container, RESULT_SLOT, 0, 1);
        } else {
            emptyGrid(container);
        }
        returnHeldItem(container);
    }

    private static boolean isGridEmpty(Container container) {
        for (int slot = FIRST_GRID_SLOT; slot < FIRST_GRID_SLOT + GRID_SLOTS; slot++) {
            if (container.inventorySlots.get(slot).getHasStack()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Puts one of every ingredient in the matching grid slot, returns false when an ingredient
     * runs out, the caller then has to clear the grid again.
     */
    private static boolean fillGrid(Container container, CraftingRecipe recipe) {
        final List<Slot> slots = container.inventorySlots;
        final int firstCarriedSlot = slots.size() - CARRIED_SLOTS;
        for (Map.Entry<ItemStack, Integer> ingredient : recipe.ingredientCounts.entrySet()) {
            final ItemStack wanted = ingredient.getKey();
            int remaining = ingredient.getValue();
            int sourceSlot = -1;
            int held = 0;
            for (int i = 0; i < recipe.items.length && remaining > 0; i++) {
                if (recipe.items[i] == null || !recipe.items[i].isItemEqual(wanted)) {
                    continue;
                }
                if (held == 0) {
                    sourceSlot = findCarriedSlot(slots, firstCarriedSlot, wanted);
                    if (sourceSlot < 0) {
                        return false;
                    }
                    held = slots.get(sourceSlot).getStack().stackSize;
                    slotClick(container, sourceSlot, 0, 0);
                }
                slotClick(container, gridSlot(i, recipe.width), 1, 0);
                held--;
                remaining--;
            }
            if (held > 0) {
                slotClick(container, sourceSlot, 0, 0);
            }
        }
        return true;
    }

    private static int findCarriedSlot(List<Slot> slots, int firstCarriedSlot, ItemStack ingredient) {
        for (int slot = firstCarriedSlot; slot < slots.size(); slot++) {
            final ItemStack carried = slots.get(slot).getStack();
            if (carried != null && CraftingRecipes.matches(ingredient, carried)) {
                return slot;
            }
        }
        return -1;
    }

    /** Moves every ingredient back into the inventory */
    private static void emptyGrid(Container container) {
        for (int slot = FIRST_GRID_SLOT; slot < FIRST_GRID_SLOT + GRID_SLOTS; slot++) {
            if (container.inventorySlots.get(slot).getHasStack()) {
                slotClick(container, slot, 0, 1);
            }
        }
    }

    /**
     * Puts whatever the player still holds back into the inventory, the click that triggered
     * the crafting would drop it on the ground otherwise.
     */
    private static void returnHeldItem(Container container) {
        if (MC.thePlayer.inventory.getItemStack() == null) {
            return;
        }
        final int firstCarriedSlot = container.inventorySlots.size() - CARRIED_SLOTS;
        for (int slot = firstCarriedSlot; slot < container.inventorySlots.size(); slot++) {
            if (container.inventorySlots.get(slot).getStack() == null) {
                slotClick(container, slot, 0, 0);
                return;
            }
        }
        MC.thePlayer.inventory.setItemStack(null);
    }

    /** Slot of the ingredient at that position of the recipe grid, offset by the result slot */
    private static int gridSlot(int index, int recipeWidth) {
        return FIRST_GRID_SLOT + index / recipeWidth * GRID_WIDTH + index % recipeWidth;
    }

    private static void slotClick(Container container, int slot, int mouseButton, int mode) {
        try {
            MC.playerController.windowClick(container.windowId, slot, mouseButton, mode, MC.thePlayer);
        } catch (RuntimeException ignored) {
        }
    }

    private static boolean isCraftable(int index) {
        final long now = System.currentTimeMillis();
        if (now - statusCheckedAt > STATUS_INTERVAL_MILLIS) {
            statusCheckedAt = now;
            for (int i = 0; i < BUTTONS.size(); i++) {
                CRAFTABLE[i] = CraftingRecipes.findCraftable(BUTTONS.get(i).targetItem, GRID_WIDTH) != null;
            }
        }
        return CRAFTABLE[index];
    }

    private static final class CraftButton extends GuiButton {

        private final int index;
        private final Item targetItem;
        private final ItemStack icon;

        private CraftButton(int index, Item targetItem) {
            super(index, 0, 0, BUTTON_SIZE, BUTTON_SIZE, "");
            this.index = index;
            this.targetItem = targetItem;
            this.icon = new ItemStack(targetItem);
        }

        @Override
        public void drawButton(Minecraft mc, int mouseX, int mouseY) {
            if (!this.visible) {
                return;
            }
            this.hovered = mouseX >= this.xPosition && mouseY >= this.yPosition
                    && mouseX < this.xPosition + this.width && mouseY < this.yPosition + this.height;
            drawIcon(mc);
            if (ExampleConfig.autoCraftStatusBadge) {
                drawCraftingStatus(isCraftable(this.index));
            }
        }

        private void drawIcon(Minecraft mc) {
            GlStateManager.pushMatrix();
            try {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableAlpha();
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                mc.getRenderItem().renderItemAndEffectIntoGUI(this.icon,
                        this.xPosition + ICON_OFFSET, this.yPosition + ICON_OFFSET);
            } finally {
                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                GlStateManager.resetColor();
                GlStateManager.popMatrix();
            }
        }

        /** Green check mark while the ingredients are there, red cross while they are not */
        private void drawCraftingStatus(boolean craftable) {
            final int x = this.xPosition + 11;
            final int y = this.yPosition + 11;
            Gui.drawRect(x - 1, y - 1, x + 9, y + 9, 0xB0000000);
            Gui.drawRect(x, y, x + 8, y + 8, craftable ? 0xFF2EAD4A : 0xFFD64545);
            if (craftable) {
                for (int i = 0; i < 3; i++) {
                    Gui.drawRect(x + 1 + i, y + 3 + i, x + 3 + i, y + 5 + i, 0xFFFFFFFF);
                    Gui.drawRect(x + 4 + i, y + 4 - i, x + 6 + i, y + 6 - i, 0xFFFFFFFF);
                }
            } else {
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        Gui.drawRect(x + 1 + i * 4, y + 1 + j * 4, x + 3 + i * 4, y + 3 + j * 4, 0xFFFFFFFF);
                    }
                }
                Gui.drawRect(x + 3, y + 3, x + 5, y + 5, 0xFFFFFFFF);
            }
        }

    }

}
