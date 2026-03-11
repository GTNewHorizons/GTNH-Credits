package net.noiraude.gtnhcredits;

import net.minecraftforge.common.MinecraftForge;
import net.noiraude.gtnhcredits.client.minecraft.gui.main_menu.GuiMainMenuHandler;

import org.jetbrains.annotations.NotNull;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(@NotNull FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getModConfigurationDirectory());
    }

    @Override
    public void init(@NotNull FMLInitializationEvent ignoredEvent) {
        MinecraftForge.EVENT_BUS.register(new GuiMainMenuHandler());
    }
}
