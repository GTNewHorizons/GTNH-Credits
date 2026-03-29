package net.noiraude.gtnhcredits.client.minecraft.gui.main_menu;

import net.noiraude.gtnhcredits.GTNHCredits;
import net.noiraude.gtnhcredits.client.gui.credits.CreditsScreen;

import com.cleanroommc.modularui.factory.ClientGUI;

import cpw.mods.fml.common.Optional;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import lumien.custommainmenu.events.SendIMCEvent;

/** Handles {@link SendIMCEvent} messages addressed to this mod. */
@SideOnly(Side.CLIENT)
public class CmmActionHandler {

    @SubscribeEvent
    @Optional.Method(modid = "custommainmenu")
    public void onSendIMC(SendIMCEvent event) {
        if (!GTNHCredits.MODID.equals(event.modId)) return;
        if ("openCredits".equals(event.message)) {
            ClientGUI.open(CreditsScreen.create());
        }
    }
}
