package net.theairblow.geolosyshax;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Logger;

@Mod(modid = GeolosysHax.MOD_ID, name = GeolosysHax.MOD_NAME, version = GeolosysHax.VERSION)
public class GeolosysHax {
    public static final String MOD_ID = "geolosyshax";
    public static final String MOD_NAME = "GeolosysHax";
    public static final String VERSION = "2.0.0";
    public static Logger LOGGER;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(MainHandler.class);
        LOGGER = event.getModLog();
    }
}
