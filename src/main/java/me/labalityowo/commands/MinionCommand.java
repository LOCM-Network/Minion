package me.labalityowo.commands;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.command.data.CommandParamType;
import cn.nukkit.command.data.CommandParameter;
import cn.nukkit.item.Item;
import cn.nukkit.item.ItemArmorStand;
import cn.nukkit.item.ItemID;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.utils.TextFormat;
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
            owner.sendMessage("/minion <player> <type> <level>");
            return true;
        }
        Player target = Server.getInstance().getPlayerExact(strings[0]);
        int type = Integer.parseInt(strings[1]);
        int level = Integer.parseInt(strings[2]);
        ItemArmorStand item = (ItemArmorStand) Item.get(ItemID.ARMOR_STAND, 0, 1);
        CompoundTag tag = new CompoundTag();
        tag.putInt("minionType", type);
        tag.putInt("minionLevel", level);
        item.setNamedTag(tag);
        target.getInventory().addItem(item);
        String atype = "";
        if(type == 1){
            atype = "Thợ mỏ";
        }else atype = (type == 2 ? "Thợ mỏ" : "Nông dân");
        item.setCustomName(TextFormat.colorize("&e" + atype + "\n&fĐặt xuống đất để tạo công nhân"));
        item.setLore(TextFormat.colorize("&l&eLưu ý:&f Làm mất admin không chịu trách nhiệm!"));
        if(target.getInventory().canAddItem(item)){
            target.getInventory().addItem(item);
            target.sendMessage(TextFormat.colorize("&l&fBạn vừa thuê công nhân thành công (&e" + atype + "&f)" ));
        }
        return true;
    }
}
