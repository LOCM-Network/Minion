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
        if(!commandSender.isOp()) return false;
        if(strings.length < 2){
            owner.sendMessage("/minion <player> <type> <level>");
            return true;
        }
        Player target = Server.getInstance().getPlayerExact(strings[0]);
        int type = Integer.parseInt(strings[1]);
        int level = Integer.parseInt(strings[2]);
        target.getInventory().addItem(Main.getMinionItem(type, level, 1));
        return true;
    }
}
