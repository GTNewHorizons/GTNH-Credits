package net.noiraude.gtnhcredits.client.minecraft.gui.main_menu;

import net.minecraft.client.gui.GuiMainMenu;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.noiraude.gtnhcredits.Config;
import net.noiraude.gtnhcredits.client.gui.credits.CreditsScreen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.cleanroommc.modularui.factory.ClientGUI;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiMainMenuHandler {

    private @Nullable CreditsButton creditsButton;

    @SubscribeEvent
    public void onGuiInit(@NotNull GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu)) return;

        // noinspection unchecked
        creditsButton = CreditsButton.create(Config.getInstance(), event.gui, event.buttonList);
    }

    @SubscribeEvent
    public void onGuiAction(@NotNull GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (!(event.gui instanceof GuiMainMenu) || event.button != creditsButton) return;

        ClientGUI.open(CreditsScreen.create());
    }

}
