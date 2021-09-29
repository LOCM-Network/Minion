package me.labalityowo.minions;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.block.BlockID;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.network.protocol.LevelEventPacket;
import cn.nukkit.utils.TextFormat;
import com.nukkitx.fakeinventories.inventory.ChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.DoubleChestFakeInventory;
import com.nukkitx.fakeinventories.inventory.FakeInventory;
import ru.contentforge.formconstructor.form.SimpleForm;
import ru.contentforge.formconstructor.form.element.Button;
import ru.contentforge.formconstructor.form.handler.SimpleFormHandler;

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

    private FakeInventory inventory;

    private MinionStatus status;

    private final int level = 1;

    private int workingTick = 0;

    private int breakingBlockTick = 0;

    private Block targetBlock;

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
        StringBuilder nametagBuilder = new StringBuilder(getOwner() + "'s Minion\n\nDEBUG: working tick: " + workingTick + "/" + namedTag.getInt("workingTick") + "\n\nStatus: ");
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
        ListTag<CompoundTag> inventoryTag = (ListTag<CompoundTag>) namedTag.getList("MinionInventory");

        inventoryTag.getAll().forEach(tag -> {
            inventory.setItem(tag.getByte("slot"), NBTIO.getItemHelper(tag));
        });

        super.initEntity();
    }

    /* PocketMine-MP */

    public void lookAt(Vector3 target){
        double horizontal = Math.sqrt(Math.pow(target.x - this.x, 2) + Math.pow(target.z - this.z, 2));
        double vertical = target.y - this.y;
        double pitch = (-Math.atan2(vertical, horizontal) / Math.PI * 180);
        double xDist = target.x - this.x;
        double zDist = target.z - this.z;
        double yaw = Math.atan2(zDist, xDist) / Math.PI * 180 - 90;
        if(this.yaw < 0){
            yaw += 360.0;
        }
        this.yaw = yaw;
        this.pitch = pitch;
    }

    @Override
    public boolean onUpdate(int currentTick) {
        boolean onUpdate = super.onUpdate(currentTick);
        if(getStatus() == MinionStatus.FULL_INVENTORY){
            return onUpdate;
        }

        if(!doToolCheck()){
            setStatus(MinionStatus.INEFFICIENT_TOOL);
            return onUpdate;
        }

        setStatus(MinionStatus.WORKING);

        if(targetBlock != null){
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
            return onUpdate;
        }

        if(workingTick >= namedTag.getInt("workingTick")){
            //TODO: Rewrite this, messy af
            Position pos = getPosition();
            for(int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Block block = getLevel().getBlock(pos.add(x, y, z));
                        if(Arrays.stream(getTargetBlocks()).anyMatch(target -> block.getId() == target.getId())){
                            lookAt(block.getLocation());
                            LevelEventPacket pk = new LevelEventPacket();
                            pk.evid = LevelEventPacket.EVENT_BLOCK_START_BREAK;
                            pk.x = (float) block.x;
                            pk.y = (float) block.y;
                            pk.z = (float) block.z;
                            pk.data = (int) (65535 / Math.ceil(block.getBreakTime(getInventory().getItemInHand()) * 20));
                            this.getLevel().addChunkPacket(block.getFloorX() >> 4, block.getFloorZ() >> 4, pk);
                            workingTick = 0;
                            targetBlock = block;
                            return onUpdate;
                        }
                    }
                }
            }
        }
        workingTick++;
        return onUpdate;
    }

    @Override
    public void saveNBT() {
        ListTag<CompoundTag> inventoryTag = new ListTag<CompoundTag>();
        inventory.getContents().forEach((index, item) -> {
            inventoryTag.add(NBTIO.putItemHelper(item, index));
        });
        namedTag.put("MinionInventory", inventoryTag);
        super.saveNBT();
    }

    public boolean doToolCheck(){
        return Arrays.stream(getTargetBlocks()).anyMatch(block -> getInventory().getItemInHand().useOn(block));
    }

    public boolean doToolCheck(Item item){
        return Arrays.stream(getTargetBlocks()).anyMatch(block -> item.useOn(block));
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
        Player damager = (Player) source1.getDamager();
        if(!(damager.getName().equals(getOwner()))){
            return true;
        }
        sendForm(damager);
        return true;
    }

    public void sendForm(Player owner){
        SimpleForm form = new SimpleForm(getOwner() + "'s minion");
        form.addButton(new Button("Change minion tool.", (p, button) -> {
            Item minionTool = getInventory().getItemInHand();
            Item playerTool = p.getInventory().getItemInHand();
            if(!doToolCheck(playerTool)){
                p.sendMessage("Ko hop item");
                return;
            }
            getInventory().setItemInHand(playerTool);
            p.getInventory().setItemInHand(minionTool);
        }));
        if(!getInventory().getItemInHand().equals(Item.get(0))){
            form.addButton(new Button("Remove minion tool.", (p, button) -> {
                Item minionTool = getInventory().getItemInHand();
                if(!p.getInventory().canAddItem(minionTool)){
                    p.sendMessage("Tui do da day");
                    return;
                }
                getInventory().setItemInHand(Item.get(0));
                p.getInventory().addItem(minionTool);
            }));
        }
        form.addButton(new Button("View minion inventory.", (p, button) -> {
            p.addWindow(inventory);
        }));
        form.addButton(new Button("Despawn minion.", (p, button) -> {
            close();
            p.sendMessage("Đã thu minion lại thành 1 item");
        }));
        form.send(owner);
    }
}
