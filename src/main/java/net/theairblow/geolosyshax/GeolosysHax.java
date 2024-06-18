package net.theairblow.geolosyshax;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.Mod;
import net.theairblow.geolosyshax.handlers.Commands;
import net.theairblow.geolosyshax.handlers.Scanner;
import org.apache.logging.log4j.Logger;

@Mod(modid = GeolosysHax.MOD_ID, name = GeolosysHax.MOD_NAME,
        version = GeolosysHax.VERSION, clientSideOnly = true)
public class GeolosysHax {
    public static final String MOD_ID = "geolosyshax";
    public static final String MOD_NAME = "GeolosysHax";
    public static final String VERSION = "2.1.0";
    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(Commands.class);
        MinecraftForge.EVENT_BUS.register(Scanner.class);
        LOGGER = event.getModLog();
    }
}
