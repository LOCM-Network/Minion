package me.labalityowo;

import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.service.RegisteredServiceProvider;
import com.nukkitx.fakeinventories.inventory.FakeInventories;
import me.labalityowo.commands.MinionCommand;
import me.labalityowo.minions.Farmer;
import me.labalityowo.minions.Miner;
import me.labalityowo.minions.Minion;
import me.labalityowo.minions.Woodcutter;

public class Main extends PluginBase implements Listener {

    private static FakeInventories inventoryManager;

    @Override
    public void onEnable() {
        RegisteredServiceProvider<FakeInventories> provider = getServer().getServiceManager().getProvider(FakeInventories.class);
        if (provider == null || provider.getProvider() == null) {
            this.getServer().getPluginManager().disablePlugin(this);
        }
        assert provider != null;
        inventoryManager = provider.getProvider();
        this.getServer().getCommandMap().register("Minion", new MinionCommand(this));
        Entity.registerEntity("Miner", Miner.class);
        Entity.registerEntity("Farmer", Farmer.class);
        Entity.registerEntity("Woodcutter", Woodcutter.class);
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    public static FakeInventories getInventoryManager() {
        return inventoryManager;
    }


    @EventHandler
    public void onInteract(PlayerInteractEvent event){
        if(event.getAction() == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK){
            Item item = event.getItem();
            CompoundTag tag = item.getNamedTag();
            if(tag == null){
                return;
            }
            if(tag.contains("minionType")){
                int type = tag.getInt("minionType");
                int level = tag.getInt("minionLevel");
                System.out.println(event.getTouchVector().toString());
                CompoundTag nbt = EntityHuman.getDefaultNBT(event.getBlock().add(0.5, 1, 0.5));
                nbt.putString("owner", event.getPlayer().getName());
                nbt.putInt("minionLevel", level);
                nbt.put("Skin", event.getPlayer().namedTag.get("Skin").copy());
                nbt.putInt("workingTick", level == 1 ? 80 : 60);
                Minion minion;
                switch (type){
                    case 1:
                        minion = new Miner(event.getPlayer().getChunk(), nbt);
                        break;
                    case 2:
                        minion = new Woodcutter(event.getPlayer().getChunk(), nbt);
                        break;
                    case 3:
                        minion = new Farmer(event.getPlayer().getChunk(), nbt);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + type);
                }
                minion.spawnToAll();
                item.setCount(item.getCount() - 1);
                event.getPlayer().getInventory().setItemInHand(item);
            }
        }
    }
}
