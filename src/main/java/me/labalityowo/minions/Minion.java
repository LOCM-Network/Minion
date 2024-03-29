package me.labalityowo.minions;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.block.Block;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.EntityDamageEvent;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArmorStand;
import cn.nukkit.item.ItemID;
import cn.nukkit.level.Position;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.NBTIO;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.ListTag;
import cn.nukkit.utils.TextFormat;
import com.nukkitx.fakeinventories.inventory.*;
import me.labalityowo.Main;
import me.locm.economyapi.EconomyAPI;
import ru.contentforge.formconstructor.form.SimpleForm;
import ru.contentforge.formconstructor.form.element.Button;

import java.util.Arrays;

public abstract class Minion extends EntityHuman {

    public enum MinionStatus{
        INEFFICIENT_TOOL,
        INEFFICIENT_WORKSTATION,
        WORKING,
        FULL_INVENTORY
    }

    public FakeInventory inventory;

    public MinionStatus status;

    public int workingTick = 0;

    public int breakingBlockTick = 0;

    public Block targetBlock;

    public Minion(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
    }

    public String getOwner(){
        return namedTag.getString("owner");
    }

    public void setStatus(MinionStatus status){
        this.status = status;
        StringBuilder nametagBuilder = new StringBuilder(getOwner() + "'s Minion\n\n&l&fTrạng thái: ");
        switch (status){
            case WORKING:
                nametagBuilder.append("&aĐang làm việc\n\n&fCông cụ:&e").append(getInventory().getItemInHand().getName());
                break;
            case FULL_INVENTORY:
                nametagBuilder.append("&cKho đầy");
                break;
            case INEFFICIENT_TOOL:
                nametagBuilder.append("&aKhông có công cụ");
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
        super.initEntity();
        setScale((float) 0.7);
        setNameTag(TextFormat.colorize(getOwner() + "'s Minion"));
        setNameTagAlwaysVisible();
        switch(namedTag.getInt("minionLevel")){
            case 1:
                inventory = new ChestFakeInventory();
                break;
            case 2:
                inventory = new DoubleChestFakeInventory();
                break;
        }
        if(namedTag.contains("MinionInventory")){
            ListTag<CompoundTag> inventoryTag = (ListTag<CompoundTag>) namedTag.getList("MinionInventory");
            inventoryTag.getAll().forEach(tag -> inventory.setItem(tag.getByte("slot"), NBTIO.getItemHelper(tag)));
        }
        if(namedTag.contains("MinionTool")){
            getInventory().setItemInHand(NBTIO.getItemHelper((CompoundTag) namedTag.get("MinionTool")));
        }
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

    public abstract void onBreak();
    public abstract int getType();
    public abstract void findBlock(Block block);

    @Override
    public boolean onUpdate(int currentTick) {
        boolean onUpdate = super.onUpdate(currentTick);
        if(getStatus() == MinionStatus.FULL_INVENTORY){
            return onUpdate;
        }

        if(!doToolCheck(getInventory().getItemInHand())){
            setStatus(MinionStatus.INEFFICIENT_TOOL);
            return onUpdate;
        }

        setStatus(MinionStatus.WORKING);

        if(targetBlock != null && checkOwner()){
            onBreak();
            return onUpdate;
        }

        if(workingTick >= namedTag.getInt("workingTick")){
            Position pos = getPosition();
            for(int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    for (int z = -3; z <= 3; z++) {
                        Block block = getLevel().getBlock(pos.add(x, y, z));
                        if(Arrays.stream(getTargetBlocks()).anyMatch(target -> block.getId() == target.getId() && block.getDamage() == target.getDamage())){
                            lookAt(block.getLocation());
                            findBlock(block);
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
        ListTag<CompoundTag> inventoryTag = new ListTag<>();
        inventory.getContents().forEach((index, item) -> {
            CompoundTag nbt = NBTIO.putItemHelper(item);
            nbt.putByte("slot", index);
            inventoryTag.add(nbt);
        });
        namedTag.put("MinionInventory", inventoryTag);
        super.saveNBT();
    }

    public boolean checkOwner(){
        String owner = this.getOwner();
        if(Server.getInstance().getPlayerExact(owner) != null){
            Player player = Server.getInstance().getPlayerExact(owner);
            return player.isOnline();
        }
        return false;
    }

    abstract public boolean doToolCheck(Item item);

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
        form.addButton(new Button(TextFormat.colorize("&l&0Đưa công cụ cho công nhân"), (p, button) -> {
            Item minionTool = getInventory().getItemInHand();
            Item playerTool = p.getInventory().getItemInHand();
            if(!doToolCheck(playerTool)){
                p.sendMessage(TextFormat.colorize("&l&cCông cụ không hợp lệ"));
                return;
            }
            getInventory().setItemInHand(playerTool);
            p.getInventory().setItemInHand(minionTool);
        }));
        if(!getInventory().getItemInHand().equals(Item.get(0))){
            form.addButton(new Button(TextFormat.colorize("&l&0Thu công cụ về"), (p, button) -> {
                Item minionTool = getInventory().getItemInHand();
                if(!p.getInventory().canAddItem(minionTool)){
                    p.sendMessage(TextFormat.colorize("&l&cTúi đã đầy"));
                    return;
                }
                getInventory().setItemInHand(Item.get(0));
                p.getInventory().addItem(minionTool);
            }));
        }
        form.addButton(new Button(TextFormat.colorize("&l&0Xem kho của công nhân"), (p, button) -> {
            FakeInventory tempInventory;
            switch(namedTag.getInt("minionLevel")){
                case 1:
                    tempInventory = new ChestFakeInventory();
                    break;
                case 2:
                    tempInventory = new DoubleChestFakeInventory();
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + level);
            }
            tempInventory.setContents(inventory.getContents());
            tempInventory.addListener(fakeSlotChangeEvent -> {
                inventory.setContents(fakeSlotChangeEvent.getAction().getInventory().getContents());
                inventory.setItem(fakeSlotChangeEvent.getAction().getSlot(), fakeSlotChangeEvent.getAction().getTargetItem());
            });
            p.addWindow(tempInventory);
        }));

        form.addButton(new Button(TextFormat.colorize("&l&0Thu công nhân về"), (p, button) -> {
            close();
            ListTag<CompoundTag> inventoryTag = new ListTag<>();
            inventory.getContents().forEach((index, item) -> {
                CompoundTag nbt = NBTIO.putItemHelper(item);
                nbt.putByte("slot", index);
                inventoryTag.add(nbt);
            });
            ItemArmorStand item = (ItemArmorStand) Main.getMinionItem(getType(), namedTag.getInt("minionLevel"), 1);
            CompoundTag tag = item.getNamedTag();
            tag.put("MinionInventory", inventoryTag);
            tag.put("MinionTool", NBTIO.putItemHelper(getInventory().getItemInHand()));
            item.setNamedTag(tag);
            owner.getInventory().addItem(item);
        }));

        if(namedTag.getInt("minionLevel") < 2){
            form.addButton(new Button(TextFormat.colorize("&l&0Nâng cấp công nhân"), (p, button) -> {
                if(EconomyAPI.getInstance().reduceCoin(p, 100) == 0){
                    p.sendMessage(TextFormat.colorize("&l&cKhông đủ LCoin để nâng cấp"));
                    return;
                }
                namedTag.putInt("minionLevel", namedTag.getInt("minionLevel") + 1);
                DoubleChestFakeInventory newInventory = new DoubleChestFakeInventory();
                newInventory.setContents(inventory.getContents());
                inventory = newInventory;
            }));
        }
        form.send(owner);
    }
}
