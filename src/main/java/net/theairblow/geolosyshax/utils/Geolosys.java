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
import net.theairblow.geolosyshax.GeolosysHax;
import scala.tools.cmd.Opt;

import java.util.*;
import java.util.stream.Collectors;

public class Geolosys {
    public static Optional<Match> getDeposit(Chunk chunk) {
        final ChunkPos chunkPos = chunk.getPos();
        final World world = chunk.getWorld();
        Set<BlockPos> visited = new HashSet<>();

        for (int x = chunkPos.getXStart(); x < chunkPos.getXEnd(); x++) {
            for (int z = chunkPos.getZStart(); z < chunkPos.getZEnd(); z++) {
                for (int y = 0; y < world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z)).getY(); y++) {
                    final BlockPos pos = new BlockPos(x, y, z);
                    if (visited.contains(pos)) continue;

                    final IBlockState state = world.getBlockState(pos);
                    for (IOre ore : GeolosysAPI.oreBlocks) {
                        if (ore instanceof DepositMultiOre) {
                            final DepositMultiOre deposit = (DepositMultiOre) ore;
                            Map<IBlockState, Set<BlockPos>> contiguousBlocks = findContiguousBlocksForMultiOre(world, pos, deposit, visited);
                            boolean allOresFound = deposit.oreBlocks.keySet().stream()
                                    .allMatch(contiguousBlocks::containsKey);

                            if (allOresFound) {
                                int totalSize = contiguousBlocks.values().stream().mapToInt(Set::size).sum();
                                if (totalSize >= Configuration.threshold) {
                                    return Optional.of(new Match(ore, pos));
                                }
                            }
                            continue;
                        }

                        if (Utils.doStatesMatch(ore.getOre(), state)) {
                            Set<BlockPos> contiguousBlocks = findContiguousBlocks(world, pos, ore.getOre(), visited);
                            if (contiguousBlocks.size() >= Configuration.threshold) {
                                return Optional.of(new Match(ore, pos));
                            }
                        }
                    }
                }
            }
        }

        return Optional.empty();
    }

    private static Set<BlockPos> findContiguousBlocks(World world, BlockPos start, IBlockState targetState, Set<BlockPos> visited) {
        Set<BlockPos> result = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            result.add(current);

            for (BlockPos neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor) && world.getBlockState(neighbor).equals(targetState)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    private static Map<IBlockState, Set<BlockPos>> findContiguousBlocksForMultiOre(World world, BlockPos start, DepositMultiOre deposit, Set<BlockPos> visited) {
        Map<IBlockState, Set<BlockPos>> result = new HashMap<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            IBlockState state = world.getBlockState(current);
            for (IBlockState oreState : deposit.oreBlocks.keySet()) {
                if (Utils.doStatesMatch(oreState, state)) {
                    result.computeIfAbsent(oreState, k -> new HashSet<>()).add(current);
                    break;
                }
            }

            for (BlockPos neighbor : getNeighbors(current)) {
                if (!visited.contains(neighbor) && deposit.oreBlocks.keySet().stream().anyMatch(oreState -> Utils.doStatesMatch(oreState, world.getBlockState(neighbor)))) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    private static List<BlockPos> getNeighbors(BlockPos pos) {
        return Arrays.asList(pos.up(), pos.down(), pos.north(), pos.south(), pos.east(), pos.west());
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
