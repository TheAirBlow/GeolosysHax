package net.theairblow.geolosyshax;

import journeymap.client.api.display.IOverlayListener;
import journeymap.client.api.display.ImageOverlay;
import journeymap.client.api.display.Waypoint;
import journeymap.client.api.util.UIState;
import net.minecraft.util.math.BlockPos;

import java.awt.geom.Point2D;
import java.util.UUID;

public class ImageListener implements IOverlayListener {
    private final ImageOverlay overlay;
    private boolean ignore = false;

    public ImageListener(ImageOverlay overlay) {
        this.overlay = overlay;
    }

    @Override
    public void onActivate(UIState mapState) {
        // Nothing to do
    }

    @Override
    public void onDeactivate(UIState mapState) {
        // Nothing to do
    }

    @Override
    public void onMouseMove(UIState mapState, Point2D.Double mousePosition, BlockPos blockPosition) {
        // Nothing to do
    }

    @Override
    public void onMouseOut(UIState mapState, Point2D.Double mousePosition, BlockPos blockPosition) {
        // Nothing to do
    }

    @Override
    public boolean onMouseClick(UIState mapState, Point2D.Double mousePosition,
                                BlockPos blockPosition, int button, boolean doubleClick) {
        if (button != 0 || !doubleClick || ignore) return false;
        Waypoint waypoint = new Waypoint(GeolosysHax.MOD_ID, UUID.randomUUID().toString(),
                overlay.getTitle().split("\n")[0].split(": ")[1],
                overlay.getDimension(), overlay.getNorthWestPoint().add(8, 0, 8));
        try {
            MapPlugin.jmAPI.remove(overlay);
            MapPlugin.jmAPI.show(waypoint);
        } catch (Exception e) {
            GeolosysHax.LOGGER.error("Failed to add waypoint: {0}", e);
        }

        ignore = true;
        return false;
    }
}
