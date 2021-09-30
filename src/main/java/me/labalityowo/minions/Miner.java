package me.labalityowo.minions;

import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AnimatePacket;
import cn.nukkit.network.protocol.LevelEventPacket;

import java.util.Arrays;

public class Miner extends Minion{
    public Miner(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public Block[] getTargetBlocks() {
        return new Block[]{
                Block.get(BlockID.STONE),
                Block.get(BlockID.COBBLE),
                Block.get(BlockID.IRON_BLOCK),
                Block.get(BlockID.IRON_ORE),
                Block.get(BlockID.COAL_BLOCK),
                Block.get(BlockID.COAL_ORE),
                Block.get(BlockID.LAPIS_BLOCK),
                Block.get(BlockID.LAPIS_ORE),
                Block.get(BlockID.GOLD_ORE),
                Block.get(BlockID.GOLD_BLOCK),
                Block.get(BlockID.DIAMOND_BLOCK),
                Block.get(BlockID.DIAMOND_ORE),
                Block.get(BlockID.EMERALD_BLOCK),
                Block.get(BlockID.EMERALD_ORE)
        };
    }

    @Override
    public void onBreak() {
        int trigger = (int) Math.ceil(targetBlock.getBreakTime(getInventory().getItemInHand())) * 10;
        if(breakingBlockTick == trigger){
            getLevel().setBlock(targetBlock, Block.get(BlockID.AIR));
            Arrays.stream(targetBlock.getDrops(getInventory().getItemInHand())).forEach(drop -> {
                if(!inventory.canAddItem(drop)){
                    setStatus(MinionStatus.FULL_INVENTORY);
                    return;
                }
                inventory.addItem(drop);
            });
            this.pitch = 0;
            targetBlock = null;
            breakingBlockTick = 0;
        }
        breakingBlockTick++;
        AnimatePacket pk = new AnimatePacket();
        pk.eid = this.getId();
        pk.action = AnimatePacket.Action.SWING_ARM;
        getLevel().addChunkPacket(getFloorX() >> 4, getFloorZ() >> 4, pk);
    }

    @Override
    public int getType() {
        return 1;
    }

    @Override
    public void findBlock(Block block) {
        LevelEventPacket pk = new LevelEventPacket();
        pk.evid = LevelEventPacket.EVENT_BLOCK_START_BREAK;
        pk.x = (float) block.x;
        pk.y = (float) block.y;
        pk.z = (float) block.z;
        pk.data = (int) (65535 / Math.ceil(block.getBreakTime(getInventory().getItemInHand()) * 20));
        getLevel().addChunkPacket(block.getFloorX() >> 4, block.getFloorZ() >> 4, pk);
        workingTick = 0;
        targetBlock = block;
    }

    @Override
    public boolean doToolCheck(Item item) {
        return item.isPickaxe();
    }
}
