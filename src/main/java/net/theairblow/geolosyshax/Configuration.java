package net.theairblow.geolosyshax;

import net.minecraftforge.common.config.Config;

@Config(modid = "geolosyshax")
public class Configuration {
    @Config.Comment("How many threads should be used for scanning chunks")
    @Config.Name("Scanner threads")
    public static int threads = 2;

    @Config.Comment("EXPERIMENTAL! Fallback to best match if no exact ore deposit match found")
    @Config.Name("Fallback to best match")
    public static boolean bestMatch = false;
}
