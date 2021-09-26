package me.labalityowo;

import cn.nukkit.Player;
import cn.nukkit.entity.EntityHuman;
import cn.nukkit.level.Position;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.plugin.service.RegisteredServiceProvider;
import com.nukkitx.fakeinventories.inventory.FakeInventories;
import me.labalityowo.commands.MinionCommand;
import me.labalityowo.minions.Miner;
import me.labalityowo.minions.Minion;
import me.labalityowo.minions.Minion.MinionType;

public class Main extends PluginBase{

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
    }

    public static FakeInventories getInventoryManager() {
        return inventoryManager;
    }

    public void spawnMinion(Player owner, Position clickPos, MinionType type, int level){
        CompoundTag nbt = EntityHuman.getDefaultNBT(clickPos);
        nbt.putString("owner", owner.getName());
        nbt.putInt("level", level);
        nbt.put("Skin", owner.namedTag.get("Skin").copy());
        Minion minion;
        switch (type){
            case Miner:
                minion = new Miner(owner.getChunk(), nbt);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + type + "This should not happen.");
        }
        minion.spawnToAll();
    }
}
