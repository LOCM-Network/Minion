package me.labalityowo.minions;

import cn.nukkit.block.*;
import cn.nukkit.item.*;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.network.protocol.AnimatePacket;

import java.util.Arrays;

public class Farmer extends Minion{

    public Farmer(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    @Override
    public Block[] getTargetBlocks() {
        return new Block[]{
                Block.get(Block.WHEAT_BLOCK),
                Block.get(Block.POTATO_BLOCK),
                Block.get(Block.COCOA_BLOCK),
                Block.get(Block.CARROT_BLOCK),
                Block.get(Block.MELON_BLOCK),
                Block.get(Block.PUMPKIN),
                Block.get(Block.LIT_PUMPKIN)
        };
    }

    @Override
    public void onBreak() {

    }

    @Override
    public int getType() {
        return 3;
    }

    @Override
    public void findBlock(Block block) {
        AnimatePacket pk = new AnimatePacket();
        pk.eid = this.getId();
        pk.action = AnimatePacket.Action.SWING_ARM;
        getLevel().addChunkPacket(getFloorX() >> 4, getFloorZ() >> 4, pk);
        if(block instanceof BlockWheat){
            getLevel().setBlock(block, Block.get(BlockID.WHEAT_BLOCK));
        }else if(block instanceof BlockBeetroot){
            getLevel().setBlock(block, Block.get(BlockID.BEETROOT_BLOCK));
        }else if(block instanceof BlockCarrot){
            getLevel().setBlock(block, Block.get(BlockID.CARROT_BLOCK));
        }else if(block instanceof BlockPotato){
            getLevel().setBlock(block, Block.get(BlockID.POTATO_BLOCK));
        }else if(block instanceof BlockNetherWart){
            getLevel().setBlock(block, Block.get(BlockID.NETHER_WART_BLOCK));
        }else{
            getLevel().setBlock(block, Block.get(BlockID.AIR));
        }
        Arrays.stream(block.getDrops(getInventory().getItemInHand())).forEach(drop -> {
            if(drop instanceof ItemSeedsWheat || drop instanceof ItemSeedsBeetroot){
                return;
            }
            if(!inventory.canAddItem(drop)){
                setStatus(MinionStatus.FULL_INVENTORY);
                return;
            }
            inventory.addItem(drop);
        });
        workingTick = 0;
        this.pitch = 0;
    }

    @Override
    public boolean doToolCheck(Item item) {
        return item.isHoe();
    }
}
