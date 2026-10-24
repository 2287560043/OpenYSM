package com.elfmcys.yesstevemodel.client.gui.button;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigCheckBoxForge extends AbstractButton {

    private final Checkbox checkbox;

    public ConfigCheckBoxForge(int x, int y, String str, ForgeConfigSpec.BooleanValue booleanValue) {
        super(x, y, 400, 20, Component.translatable("gui.yes_steve_model.config." + str));
        this.checkbox = Checkbox.builder(getMessage(), Minecraft.getInstance().font).pos(x, y).maxWidth(400).selected(booleanValue.get()).onValueChange((widget, selected) -> booleanValue.set(selected)).build();
    }

    public void onPress(InputWithModifiers event) {
        this.checkbox.onPress(event);
    }

    public boolean selected() {
        return this.checkbox.selected();
    }

    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.checkbox.setPosition(getX(), getY());
        this.checkbox.setFocused(isFocused());
        this.checkbox.active = this.active;
        this.checkbox.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void updateWidgetNarration(NarrationElementOutput output) {
        this.checkbox.updateWidgetNarration(output);
    }
}
