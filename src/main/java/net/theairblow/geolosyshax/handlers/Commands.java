package net.theairblow.geolosyshax.handlers;

import com.oitsjustjose.geolosys.common.api.GeolosysAPI;
import com.oitsjustjose.geolosys.common.api.world.IOre;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.theairblow.geolosyshax.GeolosysHax;
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
                "§2!hax found [ID] <page> - §3Lists coords of all veins with that ID\n" +
                "§2!hax found - §3Lists all ore veins found by automatic scanner\n" +
                "§2!hax notify [ID] - §3Notify when a specific ore vein is found\n" +
                "§2!hax notify clear - §3Disables all enabled notifications\n" +
                "§2!hax info [ID] - §3Prints all info about an ore vein\n" +
                "§2!hax find - §3Search for an ore vein in current chunk\n" +
                "§2!hax list - §3Lists all vein names and their IDs\n" +
                "§2!hax toggle - §3Toggles automatic chunk scanning\n" +
                "§2!hax scan - §3Manually scan all loaded chunks\n" +
                "§2!hax clear - §3Clears list of found veins");
            if (MapPlugin.hasJourneyMap())
                Chat.send("\n§cJourneyMap commands:\n" +
                    "§2!hax map toggle - §3Toggles ore vein markers\n" +
                    "§2!hax map clear - §3Removes all ore vein markers\n" +
                    "§6You can click on any sent coordinates to create a waypoint!");
            return;
        }

        switch (args[1]) {
            case "toggle": {
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
            }
            case "find": {
                Chat.sendPrefix("§6Searching for an ore vein in this chunk...");
                Scanner.manualScan(minecraft.world.getChunk(minecraft.player.chunkCoordX, minecraft.player.chunkCoordZ));
                return;
            }
            case "list": {
                Chat.send("§c====== §2List of all ore veins:§c ======");
                for (int i = 0; i < GeolosysAPI.oreBlocks.size(); i++) {
                    final IOre vein = GeolosysAPI.oreBlocks.get(i);
                    Chat.sendPrefix("§3-> §a%s §c(ID: %s)",
                        vein.getFriendlyName(), i);
                }
                break;
            }
            case "scan": {
                Chat.sendPrefix("§2All currently loaded chunks were queued.");
                ObjectSet<Long2ObjectMap.Entry<Chunk>> chunks = minecraft.world.getChunkProvider().loadedChunks.long2ObjectEntrySet();
                for (Long2ObjectMap.Entry<Chunk> chunk : chunks) Scanner.autoScan(chunk.getValue());
                break;
            }
            case "clear": {
                synchronized(Scanner.scanned) {
                    Scanner.scanned.clear();
                }

                synchronized(Scanner.veins) {
                    Scanner.veins.clear();
                }

                Chat.sendPrefix("§2Found ore veins list was successfully cleared!");
                break;
            }
            case "found":
                found(args);
                break;
            case "info":
                info(args);
                break;
            case "map":
                map(args);
                break;
            case "notify":
                notify(args);
                break;
            default:
                Chat.sendPrefix("§4Unknown command, type !hax for help.");
                break;
        }
    }

    private static void found(String[] args) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        if (args.length < 3) {
            Chat.send("§c====== §2List of all found veins§c ======");
            ArrayList<IOre> oreBlocks = GeolosysAPI.oreBlocks;
            for (int i = 0; i < oreBlocks.size(); i++) {
                IOre vein = oreBlocks.get(i);
                if (Scanner.veins.containsKey(vein)) {
                    Chat.send(new Style()
                            .setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "!hax list " + i))
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new TextComponentString("§aClick to list found vein coordinates"))),
                        "§3-> §a%s §5(ID: %s, %s veins)",
                        vein.getFriendlyName(), i, Scanner.veins.get(vein).size());
                    continue;
                }

                Chat.send("§3-> §4%s §5(ID: %s)",
                        vein.getFriendlyName(), i);
            }

            return;
        }

        try {
            final int id = Integer.parseInt(args[2]);
            final int page = args.length < 4 ? 1 : Integer.parseInt(args[3]);
            final IOre vein = (IOre)GeolosysAPI.oreBlocks.toArray()[id];
            final int pages = (int)Math.ceil(Scanner.veins.get(vein).size() / 10D);

            if (Scanner.veins.containsKey(vein)) {
                if (page > pages) {
                    Chat.sendPrefix("§4This page doesn't exist (there's only %s)", pages);
                    return;
                }

                if (page < 1) {
                    Chat.sendPrefix("§4Pages start at one and can't be negative!");
                    return;
                }

                Chat.send("§c====== §2%s §a(Page %s/%s)§c ======",
                    vein.getFriendlyName(), page, pages);
                final List<BlockPos> sorted = Scanner.veins.get(vein);
                sorted.sort(Comparator.comparingDouble(pos -> pos.distanceSq(
                    minecraft.player.posX, minecraft.player.posY, minecraft.player.posZ)));
                for (BlockPos pos : sorted.stream().skip((page - 1) * 10L)
                        .limit(10).collect(Collectors.toList())) {
                    Style style = new Style();
                    if (MapPlugin.hasJourneyMap())
                        style.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                            String.format("!hax map waypoint %s %s %s %s",
                                pos.getX(), pos.getY(), pos.getZ(), vein.getFriendlyName())))
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                new TextComponentString("§aClick to create waypoint")));
                    Chat.send(style, "§3-> §a%s %s %s",
                        pos.getX(), pos.getY(), pos.getZ());
                }

                return;
            }

            Chat.sendPrefix("§4No veins were found yet!");
        } catch (Exception e) {
            Chat.sendPrefix("§4Invalid ID or page number!");
        }
    }

    private static void info(String[] args) {
        if (args.length < 3) {
            Chat.sendPrefix("§6Usage: §c!hax info <ID>");
            return;
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
                "§2Size: §a%s",
                vein.getYMin(),
                vein.getYMax(),
                vein.getDensity(),
                vein.getChance(),
                vein.getSize());
        } catch (Exception e) {
            Chat.sendPrefix("§4Invalid ID!");
        }
    }

    private static void notify(String[] args) {
        if (args.length < 3) {
            Chat.sendPrefix("§6Usage: §c!hax notify [ID] §6/ §c!hax notify clear");
            return;
        }

        if (args[2].equals("clear")) {
            Scanner.notify.clear();
            Chat.sendPrefix("§2All ore vein notifications were disabled!");
            return;
        }

        try {
            final int id = Integer.parseInt(args[2]);
            final IOre vein = (IOre) GeolosysAPI.oreBlocks.toArray()[id];
            Scanner.notify.add(vein);
            Chat.sendPrefix("§2Enabled notifications for \"%s\"!",
                vein.getFriendlyName());
        } catch (Exception e) {
            Chat.sendPrefix("§4Invalid ID!");
        }
    }

    private static void map(String[] args) {
        if (!MapPlugin.hasJourneyMap()) {
            Chat.sendPrefix("§4JourneyMap is not installed!");
            return;
        }

        if (args.length < 3) {
            Chat.sendPrefix("§6Usage: §c!hax map [toggle/clear]");
            return;
        }

        switch (args[2]) {
            case "waypoint":
                mapWaypoint(args);
                break;
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
    }

    private static void mapWaypoint(String[] args) {
        if (args.length < 7) {
            Chat.sendPrefix("§4Unknown command, type !hax for help.");
            return;
        }

        try {
            final int x = Integer.parseInt(args[3]);
            final int y = Integer.parseInt(args[4]);
            final int z = Integer.parseInt(args[5]);
            StringBuilder name = new StringBuilder(args[6]);
            for (int i = 7; i < args.length; i++) {
                name.append(" ").append(args[i]);
            }

            MapPlugin.addWaypoint(x, y, z, name.toString());
            Chat.sendPrefix("§2Successfully created waypoint \"%s\" at %s %s %s",
                    name.toString(), x, y, z);
        } catch (Exception e) {
            GeolosysHax.LOGGER.error("Failed to create waypoint", e);
            Chat.sendPrefix("§4Failed to create a JourneyMap waypoint!");
        }
    }
}
