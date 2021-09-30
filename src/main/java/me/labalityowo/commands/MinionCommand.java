package me.labalityowo.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArmorStand;
import cn.nukkit.item.ItemID;
import cn.nukkit.nbt.tag.CompoundTag;
import me.labalityowo.Main;

public class MinionCommand extends Command {

    private Main plugin;

    public MinionCommand(Main plugin) {
        super("minion");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        Player owner = (Player) commandSender;
        if(!owner.isOp()) return false;
        if(strings.length < 2){
            owner.sendMessage("/minion <type> <level>");
            return true;
        }
        int type = Integer.parseInt(strings[0]);
        int level = Integer.parseInt(strings[1]);
        ItemArmorStand item = (ItemArmorStand) Item.get(ItemID.ARMOR_STAND, 0, 1);
        CompoundTag tag = new CompoundTag();
        tag.putInt("minionType", type);
        tag.putInt("minionLevel", level);
        item.setNamedTag(tag);
        owner.getInventory().addItem(item);
        return true;
    }
}
