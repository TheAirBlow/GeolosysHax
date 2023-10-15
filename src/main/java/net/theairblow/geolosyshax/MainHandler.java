package net.theairblow.geolosyshax;

import com.oitsjustjose.geolosys.common.api.GeolosysAPI;
import com.oitsjustjose.geolosys.common.api.world.IOre;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import journeymap.client.api.display.ImageOverlay;
import journeymap.client.api.model.MapImage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ChunkProviderClient;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainHandler {
    private static final ExecutorService executor = Executors.newFixedThreadPool(2);
    public static final List<ChunkPos> occupied = new ArrayList<>();
    private static BlockPos toHighlight = null;
    private static boolean enabled = false;
    private static long stopAt;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (stopAt >= System.currentTimeMillis())
            toHighlight = null;
        Highlighter.tick();
    }

    @SubscribeEvent
    public static void onRenderWorldLast(RenderWorldLastEvent event) {
        if (toHighlight == null) return;
        final Minecraft mc = Minecraft.getMinecraft();
        Highlighter.beginRender(mc.player, event.getPartialTicks());
        Highlighter.render(toHighlight);
        Highlighter.endRender();
    }

    @SubscribeEvent
    public static void onLoad(ChunkEvent.Load event) {
        if (!enabled) return; scanForOres(event.getChunk());
    }

    @SubscribeEvent
    public static void onExit(WorldEvent.Unload event) {
        enabled = false;
        try {
            MapPlugin.jmAPI.removeAll(GeolosysHax.MOD_ID);
        } catch (Exception e) {
            GeolosysHax.LOGGER.error("Failed to delete all markers: {0}", e);
        }
        occupied.clear();
    }

    private static void scanForOres(Chunk chunk) {
        executor.execute(() -> {
            if (occupied.contains(chunk.getPos())) return;
            final List<IBlockState> blocks = new ArrayList<>();
            final Minecraft minecraft = Minecraft.getMinecraft();
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 256; y++)
                    for (int z = 0; z < 16; z++) {
                        final IBlockState state = chunk.getBlockState(new BlockPos(x, y, z));
                        if (!blocks.contains(state)) blocks.add(state);
                    }

            if (blocks.isEmpty()) return;
            final Optional<IOre> deposit = Utilities.getDeposit(blocks);
            if (!deposit.isPresent()) return;
            final BlockPos blockPos = new BlockPos(
                    chunk.getPos().x * 16, 70, chunk.getPos().z * 16);
            BlockRendererDispatcher ren = minecraft.getBlockRendererDispatcher();
            ResourceLocation texture = new ResourceLocation(ren.getBlockModelShapes()
                    .getTexture(deposit.get().getOre()).getIconName());
            texture = new ResourceLocation(texture.getNamespace(),
                    "textures/" + texture.getPath() + ".png");
            try {
                IResource resource = minecraft.getResourceManager().getResource(texture);
                BufferedImage upscaled = Utilities.upscale(ImageIO.read(resource.getInputStream()), 16);
                MapImage icon = new MapImage(upscaled).centerAnchors().setRotation(0).setOpacity(.8f);
                ImageOverlay overlay = new ImageOverlay(GeolosysHax.MOD_ID, UUID.randomUUID().toString(),
                        blockPos, blockPos.add(16, 0, 16), icon);
                overlay.setDimension(minecraft.player.dimension)
                        .setOverlayListener(new ImageListener(overlay))
                        .setTitle("Type: " + deposit.get().getFriendlyName()
                                + "\nLocation: " + blockPos.getX() + " "
                                + blockPos.getY() + " " + blockPos.getZ()
                                + "\nDouble-click to create a waypoint!");
                occupied.add(chunk.getPos());
                minecraft.addScheduledTask(() -> {
                    try {
                        MapPlugin.jmAPI.show(overlay);
                    } catch (Exception e) {
                        GeolosysHax.LOGGER.error("Failed to add marker: {0}", e);
                    }
                });
            } catch (IOException e) {
                GeolosysHax.LOGGER.error("Failed to get texture: {0}", e);
            }
        });
    }

    private static void scanForVeins(Chunk chunk) {
        executor.execute(() -> {
            final List<IBlockState> blocks = new ArrayList<>();
            final Minecraft minecraft = Minecraft.getMinecraft();
            for (int x = 0; x < 16; x++)
                for (int y = 0; y < 256; y++)
                    for (int z = 0; z < 16; z++) {
                        final IBlockState state = chunk.getBlockState(new BlockPos(x, y, z));
                        if (!blocks.contains(state)) blocks.add(state);
                    }

            if (blocks.isEmpty()) {
                minecraft.addScheduledTask(() -> {
                    Utilities.sendToChat("§c======== §2Vein Search Result§c ========", true);
                    Utilities.sendToChat("§4This chunk does not have any ore veins!", true);
                });
                return;
            }
            final Optional<IOre> deposit = Utilities.getDeposit(blocks);
            if (!deposit.isPresent()) {
                minecraft.addScheduledTask(() -> {
                    Utilities.sendToChat("§c======== §2Vein Search Result§c ========", true);
                    Utilities.sendToChat("§4Failed to find where the vein starts!", true);
                });
                return;
            }
            final BlockPos start = Utilities.findVeinStart(chunk, deposit.get());
            stopAt = System.currentTimeMillis() + 15000; toHighlight = start;
            minecraft.addScheduledTask(() -> {
                Utilities.sendToChat("§c======== §2Vein Search Result§c ========", true);
                Utilities.sendToChat("§2Type: §a" + deposit.get().getFriendlyName(), true);
                Utilities.sendToChat("§2Location: §a" + start.getX() + " "
                        + start.getY() + " " + start.getZ(), true);
            });
        });
    }

    @SubscribeEvent
    public static void onChat(ClientChatEvent event) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final String message = event.getOriginalMessage().toLowerCase();
        if (!message.startsWith("!hax")) return;
        final String[] args = message.split(" ");
        if (args.length == 1)
            Utilities.sendToChat("§6Block highlighting will stop after 15 seconds!\n" +
                    "§2!hax info <ID> - §3Prints all info about a specific ore vein\n" +
                    "§2!hax find - §3Find the block where the vein starts\n" +
                    "§2!hax list - §3Lists all vein names and their IDs\n" +
                    "§2!hax toggle - §3Toggles chunk scanning", true);
        else switch (args[1]) {
            case "toggle":
                enabled = !enabled;
                if (!enabled) {
                    Utilities.sendToChat("§4Chunk scanning disabled, all JourneyMap markers were removed.");
                    try {
                        MapPlugin.jmAPI.removeAll(GeolosysHax.MOD_ID);
                    } catch (Exception e) {
                        GeolosysHax.LOGGER.error("Failed to delete all markers: {0}", e);
                    }
                    occupied.clear();
                } else {
                    Utilities.sendToChat("§2Chunk scanning enabled, all currently loaded chunks were queued.");
                    Long2ObjectMap<Chunk> chunks = ObfuscationReflectionHelper.getPrivateValue(
                            ChunkProviderClient.class, minecraft.world.getChunkProvider(), "field_73236_b");
                    for (Long2ObjectMap.Entry<Chunk> chunk : chunks.long2ObjectEntrySet()) scanForOres(chunk.getValue());
                }
                break;
            case "find":
                Utilities.sendToChat("§6Searching for an ore vein in this chunk...");
                scanForVeins(minecraft.world.getChunk(minecraft.player.chunkCoordX, minecraft.player.chunkCoordZ));
                break;
            case "info":
                if (args.length < 3)
                    Utilities.sendToChat("§6Usage: §c!hax info <ID>");
                else {
                    try {
                        final int id = Integer.parseInt(args[2]);
                        final IOre vein = (IOre) GeolosysAPI.oreBlocks.toArray()[id];
                        final Optional<List<String>> biomes = Utilities.listBiomes(vein);
                        Utilities.sendToChat("§c====== §2Info for §a\"" + vein.getFriendlyName() + "\"§c ======", true);
                        if (biomes.isPresent()) {
                            Utilities.sendToChat("§2Biomes list:", true);
                            for (String str : biomes.get()) {
                                Utilities.sendToChat("§3-> §a" + str, true);
                            }
                        }
                        Utilities.sendToChat("§2Y from §a" + vein.getYMin() + "§2 to §a" + vein.getYMax(), true);
                        Utilities.sendToChat("§2Density: §a" + vein.getDensity(), true);
                        Utilities.sendToChat("§2Chance: §a" + vein.getChance(), true);
                        Utilities.sendToChat("§2Size: §a" + vein.getSize(), true);
                    } catch (Exception e) {
                        Utilities.sendToChat("§4Invalid ID!");
                    }
                }
                break;
            case "list":
                Utilities.sendToChat("§c====== §2List of all ore veins:§c ======", true);
                for (int i = 0; i < GeolosysAPI.oreBlocks.size(); i++) {
                    final IOre vein = GeolosysAPI.oreBlocks.get(i);
                    Utilities.sendToChat("§3-> §a" + vein.getFriendlyName()
                            + "§c (ID: " + i + ")", true);
                }
                break;
            default:
                Utilities.sendToChat("§4Unknown command!");
                break;
        }

        event.setCanceled(true); // Cancel and add message to history
        minecraft.ingameGUI.getChatGUI().addToSentMessages(message);
    }
}
