package net.theairblow.geolosyshax.handlers;

import com.oitsjustjose.geolosys.common.api.world.IOre;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.theairblow.geolosyshax.Configuration;
import net.theairblow.geolosyshax.utils.Chat;
import net.theairblow.geolosyshax.utils.Geolosys;
import net.theairblow.geolosyshax.compat.MapPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Scanner {
    private static final ExecutorService executor = Executors.newFixedThreadPool(Configuration.threads);
    public static final HashMap<IOre, List<BlockPos>> veins = new HashMap<>();
    public static final List<ChunkPos> scanned = new ArrayList<>();
    public static final List<IOre> notify = new ArrayList<>();
    public static boolean enabled = false;

    @SubscribeEvent
    public static void onLoad(ChunkEvent.Load event) {
        if (!enabled) return;
        autoScan(event.getChunk());
    }

    @SubscribeEvent
    public static void onExit(WorldEvent.Unload event) {
        MapPlugin.disable();
        enabled = false;
    }

    public static void autoScan(Chunk chunk) {
        if (scanned.contains(chunk.getPos())) return;
        executor.execute(() -> {
            final Optional<Geolosys.Match> match = Geolosys.getDeposit(chunk);
            if (!match.isPresent()) return;
            final IOre deposit = match.get().deposit;
            final BlockPos start = match.get().start;

            if (notify.contains(deposit)) {
                Style style = new Style();
                if (MapPlugin.hasJourneyMap())
                    style.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("!hax map waypoint %s %s %s %s",
                            start.getX(), start.getY(), start.getZ(), deposit.getFriendlyName())))
                        .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new TextComponentString("§aClick to create waypoint")));
                Chat.sendPrefix(style, "§2Found \"%s\" at %s %s %s",
                    deposit.getFriendlyName(), start.getX(), start.getY(), start.getZ());
            }

            MapPlugin.addMarker(deposit, new BlockPos(
                    chunk.getPos().x * 16, 70, chunk.getPos().z * 16));

            synchronized (scanned) {
                scanned.add(chunk.getPos());
            }

            synchronized (veins) {
                if (!veins.containsKey(deposit))
                    veins.put(deposit, new ArrayList<>());
                final List<BlockPos> list = veins.get(deposit);
                if (list.contains(start)) return;
                list.add(start);
            }
        });
    }

    public static void manualScan(Chunk chunk) {
        executor.execute(() -> {
            final Minecraft minecraft = Minecraft.getMinecraft();
            final Optional<Geolosys.Match> match = Geolosys.getDeposit(chunk);

            if (!match.isPresent()) {
                minecraft.addScheduledTask(() -> {
                    Chat.send("§c======== §2Vein Search Result§c ========");
                    Chat.send("§4Failed to find an ore deposit!");
                });
                return;
            }

            final IOre deposit = match.get().deposit;
            final BlockPos start = match.get().start;
            minecraft.addScheduledTask(() -> {
                Chat.send(
                    "§c======== §2Vein Search Result§c ========\n" +
                    "§2Type: §a%s\n" +
                    "§2Location: §a%s %s %s",
                    deposit.getFriendlyName(),
                    start.getX(),
                    start.getY(),
                    start.getZ());
            });
        });
    }
}
