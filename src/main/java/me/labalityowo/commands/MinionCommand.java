package me.labalityowo.commands;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import me.labalityowo.Main;
import me.labalityowo.minions.Minion;

public class MinionCommand extends Command {

    private Main plugin;

    public MinionCommand(Main plugin) {
        super("minion");
        this.plugin = plugin;
    }

    @Override
    public boolean execute(CommandSender commandSender, String s, String[] strings) {
        Player owner = (Player) commandSender;
        this.plugin.spawnMinion(owner, owner.getPosition(), Minion.MinionType.Miner, 1);
        owner.sendMessage("Spawned debug");
        return false;
    }
}
