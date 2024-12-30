package net.theairblow.geolosyshax;

import net.minecraftforge.common.config.Config;

@Config(modid = "geolosyshax")
public class Configuration {
    @Config.Comment("How many ores should be in a chunk to be recognized as a vein.")
    @Config.Name("Ore threshold")
    public static int threshold = 50;

    @Config.Comment("How many threads should be used for scanning chunks.")
    @Config.Name("Scanner threads")
    public static int threads = 2;

    @Config.Comment("How many ore veins coordinates should be kept stored in memory.")
    @Config.Name("Maximum found list size")
    public static int maxVeins = 50;

    @Config.Comment(
        "WARNING: This feature is experimental and doesn't yield good results.\n" +
        "Fallback to best match if no exact ore deposit match found.")
    @Config.Name("Fallback to best match")
    public static boolean bestMatch = false;
}
