package net.theairblow.geolosyshax;

import com.oitsjustjose.geolosys.common.api.GeolosysAPI;
import com.oitsjustjose.geolosys.common.api.world.*;
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

public class Utilities {
    /** Fetches deposit based on a list of blocks. */
    public static Optional<IOre> getDeposit(List<IBlockState> blocks) {
        return GeolosysAPI.oreBlocks.stream().filter(x ->
                blocks.stream().anyMatch(x::oreMatches)).findFirst();
    }

    /** List all biome names from an IOre. */
    public static Optional<List<String>> listBiomes(IOre ore) {
        if (ore instanceof DepositBiomeRestricted) {
            final DepositBiomeRestricted deposit =
                    (DepositBiomeRestricted) ore;
            return Optional.of(deposit.getBiomeList().stream()
                    .map(Biome::getBiomeName).collect(Collectors.toList()));
        } else if (ore instanceof DepositMultiOreBiomeRestricted) {
            final DepositMultiOreBiomeRestricted deposit =
                    (DepositMultiOreBiomeRestricted) ore;
            return Optional.of(deposit.getBiomeList().stream()
                    .map(Biome::getBiomeName).collect(Collectors.toList()));
        } else return Optional.empty();
    }

    /** Finds where an ore vein starts, null if not found. */
    public static BlockPos findVeinStart(Chunk chunk, IOre ore) {
        for (int y = 256; y >= 0; y--)
        for (int x = 0; x < 16; x++)
        for (int z = 0; z < 16; z++) {
            final IBlockState state = chunk.getBlockState(new BlockPos(x, y, z));
            if (ore instanceof DepositMultiOre) {
                final DepositMultiOre deposit = (DepositMultiOre) ore;
                if (deposit.getOres().contains(state)) {
                    x = chunk.x * 16 + x; z = chunk.z * 16 + z;
                    return new BlockPos(x, y, z);
                }
            } else if (ore.getOre().equals(state)) {
                x = chunk.x * 16 + x; z = chunk.z * 16 + z;
                return new BlockPos(x, y, z);
            }
        }

        return null;
    }

    /** Sends message in chat with GeolosysHax prefix. */
    public static void sendToChat(String str) {
        sendToChat(str, false);
    }

    /** Sends message in chat with or without GeolosysHax prefix. */
    public static void sendToChat(String str, boolean noPrefix) {
        final Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.addScheduledTask(() -> minecraft.ingameGUI.getChatGUI().printChatMessage(
                new TextComponentString((noPrefix ? "" : "§6[§cGeolosysHax§6] ") + str)));
    }

    /** Converts a BufferedImage to a 2D RGBA array */
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

    /** Does a pixel-perfect image upscale */
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
