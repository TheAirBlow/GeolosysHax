package net.theairblow.geolosyshax;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.opengl.GL11;
import javax.vecmath.Vector3d;

public class Highlighter {
    private static int colorTicks = 0;

    /** Cube vertices for block highlighting. */
    private static Vector3d[] vertices = new Vector3d[] {
            new Vector3d(0, 0, 1),
            new Vector3d(0, 1, 1),
            new Vector3d(0, 1, 0),
            new Vector3d(0, 0, 0),
            new Vector3d(0, 0, 0),
            new Vector3d(0, 1, 0),
            new Vector3d(1, 1, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(0, 1, 1),
            new Vector3d(1, 1, 1),
            new Vector3d(1, 1, 0),
            new Vector3d(0, 1, 0),
            new Vector3d(1, 0, 1),
            new Vector3d(1, 1, 1),
            new Vector3d(0, 1, 1),
            new Vector3d(0, 0, 1),
            new Vector3d(1, 0, 0),
            new Vector3d(1, 1, 0),
            new Vector3d(1, 1, 1),
            new Vector3d(1, 0, 1),
            new Vector3d(0, 0, 0),
            new Vector3d(1, 0, 0),
            new Vector3d(1, 0, 1),
            new Vector3d(0, 0, 1)
    };

    /** Begin block highlight render. */
    public static void beginRender(EntityPlayerSP player, float partialTicks) {
        double playerX = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double playerY = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double playerZ = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;

        GL11.glPushMatrix();
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glTranslated(-playerX, -playerY, -playerZ);
    }

    /** End block highlight render. */
    public static void render(BlockPos pos) {
        final double f = .3 * Math.floor(colorTicks / 4f);
        final byte r = (byte)(Math.sin(f + 0) * 127 + 128);
        final byte g = (byte)(Math.sin(f + (2*Math.PI/3)) * 127 + 128);
        final byte b = (byte)(Math.sin(f + (4*Math.PI/3)) * 127 + 128);
        GL11.glColor4ub(r, g, b, (byte)100);
        final float mx = pos.getX();
        final float my = pos.getY();
        final float mz = pos.getZ();

        for (Vector3d vert : vertices) {
            GL11.glBegin(GL11.GL_POLYGON);
            GL11.glVertex3f(
                    mx + (float)vert.x,
                    my + (float)vert.y,
                    mz + (float)vert.z);
            GL11.glEnd();
        }
    }

    /** End block highlight render. */
    public static void endRender() {
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glPopMatrix();
    }

    /** Block highlight tick. */
    public static void tick() {
        if (colorTicks + 1 > 168)
            colorTicks = 0;
        colorTicks++;
    }
}
