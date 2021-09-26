package me.labalityowo.minions;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;

import java.util.ArrayList;

public class Miner extends Minion{
    public Miner(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public Block[] getTargetBlocks() {
        return new Block[]{Block.get(BlockID.STONE)};
    }
}
