package me.labalityowo.minions;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeInventory;

import java.util.Arrays;

public abstract class Minion extends EntityHuman {

    public enum MinionType{
        Miner,
        Woodcutter,
        Fisher,
        Farmer
    }

    public enum MinionStatus{
        INEFFICIENT_TOOL,
        INEFFICIENT_WORKSTATION,
        WORKING,
        FULL_INVENTORY
    }

    private String owner = null;
    private FakeInventory inventory;

    private MinionStatus status;

    private final int level = 1;

    public Minion(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public String getOwner(){
        return namedTag.getString("owner");
    }

    public Integer getMinionLevel(){
        return level;
    }

    public void setStatus(MinionStatus status){
        this.status = status;
        StringBuilder nametagBuilder = new StringBuilder(getOwner() + "'s Minion\n\nStatus: ");
        switch (status){
            case WORKING:
                nametagBuilder.append("Working");
                break;
            case FULL_INVENTORY:
                nametagBuilder.append("Inventory Fulled");
                break;
            case INEFFICIENT_TOOL:
                nametagBuilder.append("Inefficient tools");
                break;
            case INEFFICIENT_WORKSTATION:
                nametagBuilder.append("Inefficient workstation");
                break;
        }
        setNameTag(TextFormat.colorize(nametagBuilder.toString()));
    }

    public MinionStatus getStatus(){
        return this.status;
    }

    public abstract Block[] getTargetBlocks();

    @Override
    protected void initEntity() {
        setScale((float) 0.7);
        setNameTag(TextFormat.colorize(getOwner() + "'s Minion"));
        setNameTagAlwaysVisible();
        switch(level){
            case 1:
                inventory = new ChestFakeInventory();
                break;
            case 2:
                inventory = new DoubleChestFakeInventory();
                break;
        }
        super.initEntity();
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean onUpdate = super.onUpdate(currentTick);
        if(!inventory.canAddItem(Item.get(0))){
            setStatus(MinionStatus.FULL_INVENTORY);
            return onUpdate;
        }
        if(!(Arrays.stream(getTargetBlocks()).anyMatch(block -> getInventory().getItemInHand().useOn(block)))){
            setStatus(MinionStatus.INEFFICIENT_TOOL);
            return onUpdate;
        }
        setStatus(MinionStatus.WORKING);
        return onUpdate;
    }

    @Override
    public boolean attack(EntityDamageEvent source) {
        source.setCancelled();
        if(!(source instanceof EntityDamageByEntityEvent)){
            return true;
        }
        EntityDamageByEntityEvent source1 = (EntityDamageByEntityEvent) source;
        if(!(source1.getDamager() instanceof Player)){
            return true;
        }
        if(!(source1.getDamager().getName().equals(getOwner()))){
            return true;
        }
        System.out.println("sent inventory !!!!!");
        Player owner = ((Player) source1.getDamager());
        owner.addWindow(inventory);
        System.out.println("sent inventory !!!!!");
        return true;
    }
}
