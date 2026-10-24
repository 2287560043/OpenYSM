package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraft.client.input.MouseButtonEvent;
import com.elfmcys.yesstevemodel.YesSteveModel;
import com.elfmcys.yesstevemodel.client.gui.ISpecialWidget;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;


import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class ConfigCheckBox extends AbstractWidget implements ISpecialWidget {

    private static final Identifier location = Identifier.fromNamespaceAndPath(YesSteveModel.MOD_ID, "texture/roulette.png");

    private boolean isStateTriggered;

    private final Consumer<Boolean> consumer2;

    private final Component component2;

    public ConfigCheckBox(int x, int y, int width, Component component, Consumer<Boolean> consumer) {
        super(x, y, width, 12, Component.empty());
        this.component2 = component;
        this.consumer2 = consumer;
    }

    public ConfigCheckBox(int x, int y, Component component, Consumer<Boolean> consumer) {
        this(x, y, 115, component, consumer);
    }

    public void extractWidgetRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.nextStratum();

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, location, getX(), getY(), this.isStateTriggered ? 128.0f : 0.0f, isHoveredOrFocused() ? 12.0f : 0.0f, this.width, this.height, 256, 256);
        guiGraphics.text(Minecraft.getInstance().font, this.component2, getX() + 14, getY() + 2, -1, false);
    }

    public void setStateTriggered(boolean stateTriggered) {
        this.isStateTriggered = stateTriggered;
    }

    @Override
    public void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        this.isStateTriggered = !this.isStateTriggered;
        this.consumer2.accept(Boolean.valueOf(this.isStateTriggered));
    }
}