package net.theairblow.geolosyshax.utils;

import com.oitsjustjose.geolosys.common.api.GeolosysAPI;
import com.oitsjustjose.geolosys.common.api.world.DepositBiomeRestricted;
import com.oitsjustjose.geolosys.common.api.world.DepositMultiOre;
import com.oitsjustjose.geolosys.common.api.world.DepositMultiOreBiomeRestricted;
import com.oitsjustjose.geolosys.common.api.world.IOre;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class Image {
    private static int[][] convertToPixels(BufferedImage image) {
        final byte[] pixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        final int width = image.getWidth();
        final int height = image.getHeight();
        final boolean hasAlphaChannel = image.getAlphaRaster() != null;

        int[][] result = new int[height][width];
        if (hasAlphaChannel) {
            final int pixelLength = 4;
            for (int pixel = 0, row = 0, col = 0; pixel + 3 < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += (((int) pixels[pixel] & 0xff) << 24); // alpha
                argb += ((int) pixels[pixel + 1] & 0xff); // blue
                argb += (((int) pixels[pixel + 2] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 3] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        } else {
            final int pixelLength = 3;
            for (int pixel = 0, row = 0, col = 0; pixel + 2 < pixels.length; pixel += pixelLength) {
                int argb = 0;
                argb += -16777216; // 255 alpha
                argb += ((int) pixels[pixel] & 0xff); // blue
                argb += (((int) pixels[pixel + 1] & 0xff) << 8); // green
                argb += (((int) pixels[pixel + 2] & 0xff) << 16); // red
                result[row][col] = argb;
                col++;
                if (col == width) {
                    col = 0;
                    row++;
                }
            }
        }
        return result;
    }

    public static BufferedImage upscale(BufferedImage image, int multiplier) {
        int[][] pixels = convertToPixels(image);
        int width = image.getWidth();
        int height = image.getHeight();

        BufferedImage imageResult = new BufferedImage(width * multiplier,
                height * multiplier, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width * multiplier; x ++)
            for (int y = 0; y < height * multiplier; y++)
                imageResult.setRGB(x, y, pixels[y/ multiplier][x/ multiplier]);
        return imageResult;
    }
}
