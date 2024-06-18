package net.theairblow.geolosyshax.handlers;

import com.oitsjustjose.geolosys.common.api.GeolosysAPI;
import com.oitsjustjose.geolosys.common.api.world.IOre;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.theairblow.geolosyshax.utils.Chat;
import net.theairblow.geolosyshax.utils.Geolosys;
import net.theairblow.geolosyshax.compat.MapPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class Commands {
    @SubscribeEvent
    public static void onChat(ClientChatEvent event) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        final String message = event.getOriginalMessage().toLowerCase();
        if (!message.startsWith("!hax")) return;
        event.setCanceled(true);
        minecraft.ingameGUI.getChatGUI().addToSentMessages(message);
        final String[] args = message.split(" ");
        if (args.length == 1) {
            Chat.send("§cGeolosysHax commands:\n" +
                "§2!hax found <ID> <page> - §3Lists coords of all veins with that ID\n" +
                "§2!hax found list - §3Lists all ore veins found by automatic scanner\n" +
                "§2!hax info <ID> - §3Prints all info about a specific ore vein\n" +
                "§2!hax find - §3Search for an ore vein in current chunk\n" +
                "§2!hax list - §3Lists all vein names and their IDs\n" +
                "§2!hax toggle - §3Toggles automatic chunk scanning");
            if (MapPlugin.hasJourneyMap())
                Chat.send("\n§cJourneyMap commands:\n" +
                    "§2!hax map toggle - §3Toggles ore vein markers\n" +
                    "§2!hax map clear - §3Removes all ore vein markers");
            return;
        }

        switch (args[1]) {
            case "found":
                if (args.length < 3) {
                    Chat.sendPrefix("§6Usage: §c!hax found <ID> <page> §6/ §c!hax found list");
                    break;
                }

                if (args[2].equals("list")) {
                    Chat.send("§c====== §2List of all found veins§c ======");
                    ArrayList<IOre> oreBlocks = GeolosysAPI.oreBlocks;
                    for (int i = 0; i < oreBlocks.size(); i++) {
                        IOre vein = oreBlocks.get(i);
                        if (Scanner.veins.containsKey(vein)) {
                            Chat.send("§3-> §a%s §5(ID: %s, %s veins)",
                                vein.getFriendlyName(), i, Scanner.veins.get(vein).size());
                            continue;
                        }

                        Chat.send("§3-> §4%s §5(ID: %s)",
                            vein.getFriendlyName(), i);
                    }
                    break;
                }

                try {
                    final int id = Integer.parseInt(args[2]);
                    final int page = args.length < 4 ? 1 : Integer.parseInt(args[3]);
                    final IOre vein = (IOre)GeolosysAPI.oreBlocks.toArray()[id];
                    final int pages = (int)Math.ceil(Scanner.veins.get(vein).size() / 10D);

                    if (Scanner.veins.containsKey(vein)) {
                        if (page > pages) {
                            Chat.sendPrefix("§4This page doesn't exist (there's only %s)", pages);
                            break;
                        }

                        if (page < 1) {
                            Chat.sendPrefix("§4Pages start at one and can't be negative!");
                            break;
                        }

                        Chat.send("§c====== §2%s §a(Page %s/%s)§c ======",
                            vein.getFriendlyName(), page, pages);
                        for (BlockPos pos : Scanner.veins.get(vein).stream().skip((page - 1) * 10L)
                                .limit(10).collect(Collectors.toList()))
                            Chat.send("§3-> §a%s %s %s",
                                pos.getX(), pos.getY(), pos.getZ());
                        break;
                    }

                    Chat.sendPrefix("§4No veins were found yet!");
                } catch (Exception e) {
                    Chat.sendPrefix("§4Invalid ID or page number!");
                }
                break;
            case "toggle":
                if (Scanner.enabled) {
                    Chat.sendPrefix("§4Automatic chunk scanning has been disabled.");
                    Scanner.enabled = false;
                    break;
                }

                Chat.sendPrefix("§2Chunk scanning enabled, all currently loaded chunks were queued.");
                ObjectSet<Long2ObjectMap.Entry<Chunk>> chunks = minecraft.world.getChunkProvider().loadedChunks.long2ObjectEntrySet();
                for (Long2ObjectMap.Entry<Chunk> chunk : chunks) Scanner.autoScan(chunk.getValue());
                Scanner.enabled = true;
                break;
            case "find":
                Chat.sendPrefix("§6Searching for an ore vein in this chunk...");
                Scanner.manualScan(minecraft.world.getChunk(minecraft.player.chunkCoordX, minecraft.player.chunkCoordZ));
                break;
            case "list":
                Chat.send("§c====== §2List of all ore veins:§c ======");
                for (int i = 0; i < GeolosysAPI.oreBlocks.size(); i++) {
                    final IOre vein = GeolosysAPI.oreBlocks.get(i);
                    Chat.sendPrefix("§3-> §a%s §c(ID: %s)",
                            vein.getFriendlyName(), i);
                }
                break;
            case "info":
                if (args.length < 3) {
                    Chat.sendPrefix("§6Usage: §c!hax info <ID>");
                    break;
                }

                try {
                    final int id = Integer.parseInt(args[2]);
                    final IOre vein = (IOre) GeolosysAPI.oreBlocks.toArray()[id];
                    final Optional<List<String>> biomes = Geolosys.listBiomes(vein);
                    Chat.send(
                        "§c====== §2Info for §a\"%s\"§c ======",
                        vein.getFriendlyName());
                    if (biomes.isPresent()) {
                        Chat.send("§2Biomes list:");
                        for (String str : biomes.get())
                            Chat.send("§3-> §a%s", str);
                    }
                    Chat.send(
                        "§2Y from §a%s§2 to §a%s\n" +
                        "§2Density: §a%s\n" +
                        "§2Chance: §a%s\n" +
                        "§2Size: §a%s\n",
                        vein.getYMin(),
                        vein.getYMax(),
                        vein.getDensity(),
                        vein.getChance(),
                        vein.getSize());
                } catch (Exception e) {
                    Chat.sendPrefix("§4Invalid ID!");
                }
                break;
            case "map":
                if (!MapPlugin.hasJourneyMap()) {
                    Chat.sendPrefix("§4JourneyMap is not installed!");
                    break;
                }

                if (args.length < 3) {
                    Chat.sendPrefix("§6Usage: §c!hax map [toggle/clear]");
                    break;
                }

                switch (args[2]) {
                    case "toggle":
                        if (MapPlugin.isEnabled()) {
                            Chat.sendPrefix("§4Ore vein markers were disabled.");
                            MapPlugin.disable();
                            break;
                        }

                        Chat.sendPrefix("§2Ore vein markers were enabled.");
                        MapPlugin.enable();
                        break;
                    case "clear":
                        Chat.sendPrefix("§2All created markers were removed.");
                        MapPlugin.removeMarkers();
                        break;
                    default:
                        Chat.sendPrefix("§6Usage: §c!hax map [toggle/clear]");
                        break;
                }
                break;
            default:
                Chat.sendPrefix("§4Unknown command, type !hax for help.");
                break;
        }
    }
}
