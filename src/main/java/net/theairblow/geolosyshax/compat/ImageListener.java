package net.theairblow.geolosyshax.compat;

import journeymap.client.api.display.IOverlayListener;
import journeymap.client.api.display.ImageOverlay;
import journeymap.client.api.util.UIState;
import net.minecraft.util.math.BlockPos;

import java.awt.geom.Point2D;

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
        MapPlugin.addWaypoint(overlay);
        ignore = true;
        return true;
    }
}
