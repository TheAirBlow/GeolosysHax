package net.theairblow.geolosyshax.utils;

import com.oitsjustjose.geolosys.common.api.GeolosysAPI;
import com.oitsjustjose.geolosys.common.api.world.*;
import com.oitsjustjose.geolosys.common.util.Utils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.theairblow.geolosyshax.Configuration;
import scala.tools.cmd.Opt;

import java.util.*;
import java.util.stream.Collectors;

public class Geolosys {
    public static Optional<Match> getDeposit(Chunk chunk) {
        final ChunkPos chunkPos = chunk.getPos();
        final World world = chunk.getWorld();
        HashMap<IOre, List<IBlockState>> foundMap = new HashMap<>();
        for (IOre ore : GeolosysAPI.oreBlocks) {
            if (!(ore instanceof DepositMultiOre)) continue;
            foundMap.put(ore, new ArrayList<>());
        }

        for (int x = chunkPos.getXStart(); x < chunkPos.getXEnd(); x++)
        for (int z = chunkPos.getZStart(); z < chunkPos.getZEnd(); z++)
        for (int y = 0; y < world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY(); y++) {
            final BlockPos pos = new BlockPos(x, y, z);
            final IBlockState state = world.getBlockState(pos);
            for (IOre ore : GeolosysAPI.oreBlocks) {
                if (ore instanceof DepositMultiOre) {
                    final DepositMultiOre deposit = (DepositMultiOre) ore;
                    for (IBlockState oreState : deposit.oreBlocks.keySet()) {
                        if (!Utils.doStatesMatch(oreState, state)) continue;
                        if (foundMap.get(ore).stream().anyMatch(a -> Utils.doStatesMatch(oreState, a))) break;
                        foundMap.get(ore).add(state);
                        if (foundMap.get(ore).size() == deposit.oreBlocks.size())
                            return Optional.of(new Match(ore, pos));
                        break;
                    }

                    continue;
                }

                if (Utils.doStatesMatch(ore.getOre(), state))
                    return Optional.of(new Match(ore, pos));
            }
        }

        if (Configuration.bestMatch) {
            final Optional<Map.Entry<IOre, List<IBlockState>>> bestMatch =
                    foundMap.entrySet().stream().filter(x -> !x.getValue().isEmpty()).findFirst();
            if (bestMatch.isPresent())
                return Optional.of(new Match(bestMatch.get().getKey(),
                    new BlockPos(chunk.x * 16 + 8, 70, chunk.z * 16 + 8)));
        }

        return Optional.empty();
    }

    public static Optional<List<String>> listBiomes(IOre ore) {
        if (ore instanceof DepositBiomeRestricted) {
            final DepositBiomeRestricted deposit =
                    (DepositBiomeRestricted) ore;
            return Optional.of(deposit.getBiomeList().stream()
                    .map(Biome::getBiomeName).collect(Collectors.toList()));
        }

        if (ore instanceof DepositMultiOreBiomeRestricted) {
            final DepositMultiOreBiomeRestricted deposit =
                    (DepositMultiOreBiomeRestricted) ore;
            return Optional.of(deposit.getBiomeList().stream()
                    .map(Biome::getBiomeName).collect(Collectors.toList()));
        }

        return Optional.empty();
    }

    public static class Match {
        public IOre deposit;
        public BlockPos start;

        public Match(IOre deposit, BlockPos start) {
            this.deposit = deposit; this.start = start;
        }
    }
}
