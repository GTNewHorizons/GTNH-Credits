package net.noiraude.gtnhcredits;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = GTNHCredits.MODID, version = Tags.VERSION, name = "GTNH Credits", acceptedMinecraftVersions = "[1.7.10]")
public class GTNHCredits {

    public static final String MODID = "gtnhcredits";
    public static Logger LOG;

    @SidedProxy(
        clientSide = "net.noiraude.gtnhcredits.ClientProxy",
        serverSide = "net.noiraude.gtnhcredits.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOG = event.getModLog();
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
