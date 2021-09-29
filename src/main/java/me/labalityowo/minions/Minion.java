package me.labalityowo.minions;

import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
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

    private int triggerAfterSecond = 0;

    private int workingTick = 0;

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
                triggerAfterSecond = 3 * 100;
                break;
            case 2:
                inventory = new DoubleChestFakeInventory();
                triggerAfterSecond = 2 * 100;
                break;
        }
        super.initEntity();
    }

    /* PocketMine-MP */

    public void lookAt(Vector3 target){
        double horizontal = Math.sqrt(Math.pow(target.x - this.x, 2) + Math.pow(target.z - this.z, 2));
        double vertical = target.y - this.y;
        setPitch(-Math.atan2(vertical, horizontal) / Math.PI * 180);
        double xDist = target.x - this.x;
        double zDist = target.z - this.z;
        setYaw(Math.atan2(zDist, xDist) / Math.PI * 180 - 90);
        if(getYaw() < 0){
            setYaw(getYaw() + 360.0);
        }
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

        if(workingTick == triggerAfterSecond){
            //TODO: Rewrite this, messy af
            Position pos = getPosition();
            for(int x = -3; x < 4; x++) {
                for (int y = -3; y < 4; y++) {
                    for (int z = -3; z < 4; z++) {
                        Block block = getLevel().getBlock(pos.add(x, y, z));
                        if(Arrays.stream(getTargetBlocks()).anyMatch(target -> block.getId() == target.getId())){
                            block.onBreak(getInventory().getItemInHand());
                            Item[] drops = block.getDrops(getInventory().getItemInHand());
                            Arrays.stream(drops).forEach(drop -> {
                                if(!inventory.canAddItem(drop)){
                                    setStatus(MinionStatus.FULL_INVENTORY);
                                    return;
                                }
                                inventory.addItem(drop);
                            });
                            break;
                        }
                    }
                }
            }
            workingTick = 0;
            return onUpdate;
        }
        workingTick++;
        return onUpdate;
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
