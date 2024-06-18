package net.theairblow.geolosyshax.compat;

import com.oitsjustjose.geolosys.common.api.world.DepositMultiOre;
import com.oitsjustjose.geolosys.common.api.world.IOre;
import journeymap.client.api.ClientPlugin;
import journeymap.client.api.IClientAPI;
import journeymap.client.api.IClientPlugin;
import journeymap.client.api.display.ImageOverlay;
import journeymap.client.api.display.Waypoint;
import journeymap.client.api.event.ClientEvent;
import journeymap.client.api.model.MapImage;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.theairblow.geolosyshax.GeolosysHax;
import net.theairblow.geolosyshax.utils.Geolosys;
import net.theairblow.geolosyshax.utils.Image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ClientPlugin
public class MapPlugin implements IClientPlugin {
    private static final List<ImageOverlay> markers = new ArrayList<>();
    private static IClientAPI jmAPI = null;
    private static boolean enabled = false;

    public static boolean hasJourneyMap() {
        return jmAPI != null;
    }

    public static boolean isEnabled() {
        return jmAPI != null && enabled;
    }

    public static void disable() {
        if (jmAPI == null) return;
        jmAPI.removeAll(GeolosysHax.MOD_ID);
        enabled = false;
    }

    public static void enable() {
        if (jmAPI == null) return;
        enabled = true;
        for (ImageOverlay marker : markers) {
            try {
                jmAPI.show(marker);
            } catch (Exception e) {
                GeolosysHax.LOGGER.error("Failed to add marker: {0}", e);
            }
        }
    }

    public static void removeMarkers() {
        jmAPI.removeAll(GeolosysHax.MOD_ID);
        markers.clear();
    }

    public static void addWaypoint(ImageOverlay overlay) {
        if (!hasJourneyMap()) return;
        Waypoint waypoint = new Waypoint(
            GeolosysHax.MOD_ID, UUID.randomUUID().toString(),
            overlay.getTitle().split("\n")[0].split(": ")[1],
            overlay.getDimension(), overlay.getNorthWestPoint().add(8, 0, 8));
        try {
            jmAPI.remove(overlay);
            jmAPI.show(waypoint);
        } catch (Exception e) {
            GeolosysHax.LOGGER.error("Failed to add waypoint: {0}", e);
        }
    }

    public static void addWaypoint(int x, int y, int z, String name) {
        if (!hasJourneyMap()) return;
        final Minecraft minecraft = Minecraft.getMinecraft();
        Waypoint waypoint = new Waypoint(
            GeolosysHax.MOD_ID, UUID.randomUUID().toString(),
            name, minecraft.world.provider.getDimension(),
            new BlockPos(x, y, z));
        try {
            jmAPI.show(waypoint);
        } catch (Exception e) {
            GeolosysHax.LOGGER.error("Failed to add waypoint: {0}", e);
        }
    }

    public static void addMarker(IOre deposit, BlockPos blockPos) {
        if (!isEnabled()) return;
        try {
            final Minecraft minecraft = Minecraft.getMinecraft();
            final BlockRendererDispatcher ren = minecraft.getBlockRendererDispatcher();
            final IBlockState ore = deposit instanceof DepositMultiOre
                    ? ((DepositMultiOre) deposit).oreBlocks.keySet().stream().findFirst().get()
                    : deposit.getOre();
            ResourceLocation texture = new ResourceLocation(
                    ren.getBlockModelShapes().getTexture(ore).getIconName());
            texture = new ResourceLocation(texture.getNamespace(),
                    "textures/" + texture.getPath() + ".png");
            final IResource resource = minecraft.getResourceManager().getResource(texture);
            final BufferedImage upscaled = Image.upscale(ImageIO.read(resource.getInputStream()), 16);
            final MapImage icon = new MapImage(upscaled).centerAnchors().setRotation(0).setOpacity(.8f);
            final ImageOverlay overlay = new ImageOverlay(GeolosysHax.MOD_ID, UUID.randomUUID().toString(),
                    blockPos, blockPos.add(16, 0, 16), icon);
            overlay.setDimension(minecraft.player.dimension).setOverlayListener(new ImageListener(overlay))
                    .setTitle(String.format("Type: %s\nLocation: %s %s %s\nDouble-click to create a waypoint!",
                            deposit.getFriendlyName(), blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            minecraft.addScheduledTask(() -> {
                try {
                    jmAPI.show(overlay);
                    markers.add(overlay);
                } catch (Exception e) {
                    GeolosysHax.LOGGER.error("Failed to add marker: {0}", e);
                }
            });
        } catch (IOException e) {
            GeolosysHax.LOGGER.error("Failed to get texture: {0}", e);
        }
    }

    @Override
    public void initialize(IClientAPI jmClientApi) {
        jmAPI = jmClientApi;
    }

    @Override
    public String getModId() {
        return GeolosysHax.MOD_ID;
    }

    @Override
    public void onEvent(ClientEvent event) {
        // We don't have anything to listen for.
    }
}
