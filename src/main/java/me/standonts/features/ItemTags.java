package me.standonts.features;

import me.standonts.config.ExampleConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Items;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;

public final class ItemTags {

    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final double MAX_DISTANCE_SQUARED = 50.0D * 50.0D;
    private static final float TAG_SCALE = 0.02666667F;

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        if (!ExampleConfig.itemTagsEnabled || MC.theWorld == null || MC.thePlayer == null) {
            return;
        }

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(1.0F, -1000000.0F);
        try {
            for (Entity entity : MC.theWorld.loadedEntityList) {
                if (!(entity instanceof EntityItem) || entity.isDead) {
                    continue;
                }

                EntityItem entityItem = (EntityItem) entity;
                ItemStack stack = entityItem.getEntityItem();
                if (stack == null) {
                    continue;
                }

                double distanceSquared = MC.thePlayer.getDistanceSqToEntity(entityItem);
                if (distanceSquared > MAX_DISTANCE_SQUARED) {
                    continue;
                }

                String tag = getTag(stack);
                if (tag == null) {
                    continue;
                }

                StringBuilder text = new StringBuilder(tag);
                if (ExampleConfig.itemTagsShowCount) {
                    text.append(" \u00A7fx").append(stack.stackSize);
                }
                if (ExampleConfig.itemTagsShowDistance) {
                    text.append(" \u00A7c")
                            .append((int) Math.sqrt(distanceSquared))
                            .append('m');
                }
                renderTag(entityItem, text.toString(), event.partialTicks);
            }
        } finally {
            GL11.glPolygonOffset(1.0F, 1000000.0F);
            GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
        }
    }

    private String getTag(ItemStack stack) {
        Item item = stack.getItem();
        String itemName = stack.getDisplayName();
        String plainName = StringUtils.stripControlCodes(itemName);

        if (plainName.startsWith("Phoenix's Tears of Regen")
                && ExampleConfig.showPhoenixTears) {
            return itemName;
        }
        if (plainName.startsWith("Squid's Absorption")
                && ExampleConfig.showSquidAbsorption) {
            return itemName;
        }
        if (plainName.startsWith("Matey") && ExampleConfig.showMiscItemTags) {
            return itemName;
        }
        if (plainName.startsWith("Regen-Ade") && ExampleConfig.showRegenAde) {
            return itemName;
        }
        if (plainName.startsWith("Ultra Pasteurized Milk Bucket")
                && ExampleConfig.showMilkBucket) {
            return itemName;
        }
        if (plainName.startsWith("Junk Apple") && ExampleConfig.showMiscItemTags) {
            return itemName;
        }
        if (item == Items.golden_apple && ExampleConfig.showGoldenApples) {
            return stack.getRarity() == EnumRarity.EPIC ? "\u00A76\u00A7lNotch Apple" : itemName;
        }
        if (isDiamondItem(item) && ExampleConfig.showDiamondItems) {
            return getDiamondColor(stack) + itemName;
        }
        return null;
    }

    private boolean isDiamondItem(Item item) {
        return item == Items.diamond_boots
                || item == Items.diamond_chestplate
                || item == Items.diamond_helmet
                || item == Items.diamond_leggings
                || item == Items.diamond_sword
                || item == Items.diamond;
    }

    private String getDiamondColor(ItemStack stack) {
        if (stack.getItem() == Items.diamond_sword) {
            int sharpness = EnchantmentHelper.getEnchantmentLevel(
                    Enchantment.sharpness.effectId, stack);
            int fireAspect = EnchantmentHelper.getEnchantmentLevel(
                    Enchantment.fireAspect.effectId, stack);
            if (sharpness >= 4) {
                return "\u00A7c";
            }
            if (sharpness == 3) {
                return "\u00A7e";
            }
            if (fireAspect >= 1) {
                return "\u00A74";
            }
        }
        return "\u00A7b";
    }

    private void renderTag(EntityItem entityItem, String text, float partialTicks) {
        FontRenderer font = MC.fontRendererObj;
        double x = interpolate(entityItem.lastTickPosX, entityItem.posX, partialTicks)
                - MC.getRenderManager().viewerPosX;
        double y = interpolate(entityItem.lastTickPosY, entityItem.posY, partialTicks)
                - MC.getRenderManager().viewerPosY;
        double z = interpolate(entityItem.lastTickPosZ, entityItem.posZ, partialTicks)
                - MC.getRenderManager().viewerPosZ;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y + entityItem.height + 0.5D, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-MC.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        float viewX = MC.gameSettings.thirdPersonView == 2
                ? -MC.getRenderManager().playerViewX
                : MC.getRenderManager().playerViewX;
        GlStateManager.rotate(viewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.scale(-TAG_SCALE, -TAG_SCALE, TAG_SCALE);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        int halfWidth = font.getStringWidth(text) / 2;
        if (ExampleConfig.itemTagsShowBackground) {
            drawBackground(halfWidth);
        }
        font.drawString(text, -halfWidth, 0, 0xFFFFFFFF);

        GlStateManager.enableTexture2D();
        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private void drawBackground(int halfWidth) {
        GlStateManager.disableTexture2D();
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer renderer = tessellator.getWorldRenderer();
        renderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos(-halfWidth - 1, -1.0D, 0.0D)
                .color(0.0F, 0.0F, 0.0F, 0.35F).endVertex();
        renderer.pos(-halfWidth - 1, 8.0D, 0.0D)
                .color(0.0F, 0.0F, 0.0F, 0.35F).endVertex();
        renderer.pos(halfWidth + 1, 8.0D, 0.0D)
                .color(0.0F, 0.0F, 0.0F, 0.35F).endVertex();
        renderer.pos(halfWidth + 1, -1.0D, 0.0D)
                .color(0.0F, 0.0F, 0.0F, 0.35F).endVertex();
        tessellator.draw();
        GlStateManager.enableTexture2D();
    }

    private double interpolate(double previous, double current, float partialTicks) {
        return previous + (current - previous) * partialTicks;
    }
}
