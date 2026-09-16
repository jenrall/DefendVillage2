package com.example.dv;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DVCommand implements CommandExecutor {

    private final DVPlugin plugin;

    public DVCommand(DVPlugin plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Only players.");
            return true;
        }

        if (args.length == 0) { help(p); return true; }

        switch (args[0].toLowerCase()) {
            case "help" -> help(p);
            case "list" -> list(p);
            case "create" -> create(p, args);
            case "delete" -> delete(p, args);
            case "pos1" -> pos1(p, args);
            case "pos2" -> pos2(p, args);
            case "addvillager" -> addVillager(p, args);
            case "addspawn" -> addSpawn(p, args);
            case "start" -> start(p, args);
            case "stop" -> stop(p);
            case "join" -> plugin.joinGame(p);
            case "leave" -> plugin.leaveGame(p);
            case "invite" -> invite(p, args);
            default -> p.sendMessage(Component.text("Unknown. /dv help", NamedTextColor.RED));
        }
        return true;
    }

    private void help(Player p) {
        p.sendMessage(Component.text("=== Defend Village ===", NamedTextColor.GOLD));
        p.sendMessage(Component.text("/dv create <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv delete <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv list", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv pos1 <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv pos2 <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv addvillager <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv addspawn <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv start <name>", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv stop", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv join", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv leave", NamedTextColor.YELLOW));
        p.sendMessage(Component.text("/dv invite <player>", NamedTextColor.YELLOW));
    }

    private void list(Player p) {
        if (plugin.getArenas().isEmpty()) {
            p.sendMessage(Component.text("No arenas.", NamedTextColor.RED));
            return;
        }
        p.sendMessage(Component.text("=== Arenas ===", NamedTextColor.GOLD));
        plugin.getArenas().forEach((name, a) ->
            p.sendMessage(Component.text("- " + name
                + " | villagers: " + a.getVillagerSpots().size()
                + " | spawns: " + a.getZombieSpawns().size(), NamedTextColor.YELLOW))
        );
    }

    private void create(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv create <name>", NamedTextColor.RED)); return; }
        if (plugin.createArena(args[1])) {
            p.sendMessage(Component.text("Arena created. Now pos1/pos2/addvillager/addspawn", NamedTextColor.GREEN));
        } else {
            p.sendMessage(Component.text("Arena exists.", NamedTextColor.RED));
        }
    }

    private void delete(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv delete <name>", NamedTextColor.RED)); return; }
        if (plugin.deleteArena(args[1])) {
            p.sendMessage(Component.text("Deleted.", NamedTextColor.GREEN));
        } else {
            p.sendMessage(Component.text("Not found.", NamedTextColor.RED));
        }
    }

    private void pos1(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv pos1 <name>", NamedTextColor.RED)); return; }
        DVPlugin.Arena a = plugin.getArena(args[1]);
        if (a == null) { p.sendMessage(Component.text("Not found.", NamedTextColor.RED)); return; }
        a.setPos1(p.getLocation());
        p.sendMessage(Component.text("pos1 set.", NamedTextColor.GREEN));
    }

    private void pos2(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv pos2 <name>", NamedTextColor.RED)); return; }
        DVPlugin.Arena a = plugin.getArena(args[1]);
        if (a == null) { p.sendMessage(Component.text("Not found.", NamedTextColor.RED)); return; }
        a.setPos2(p.getLocation());
        p.sendMessage(Component.text("pos2 set.", NamedTextColor.GREEN));
    }

    private void addVillager(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv addvillager <name>", NamedTextColor.RED)); return; }
        DVPlugin.Arena a = plugin.getArena(args[1]);
        if (a == null) { p.sendMessage(Component.text("Not found.", NamedTextColor.RED)); return; }
        a.addVillagerSpot(p.getLocation());
        p.sendMessage(Component.text("Villager spot added (" + a.getVillagerSpots().size() + ")", NamedTextColor.GREEN));
    }

    private void addSpawn(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv addspawn <name>", NamedTextColor.RED)); return; }
        DVPlugin.Arena a = plugin.getArena(args[1]);
        if (a == null) { p.sendMessage(Component.text("Not found.", NamedTextColor.RED)); return; }
        a.addZombieSpawn(p.getLocation());
        p.sendMessage(Component.text("Zombie spawn added (" + a.getZombieSpawns().size() + ")", NamedTextColor.GREEN));
    }

    private void start(Player p, String[] args) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv start <name>", NamedTextColor.RED)); return; }
        if (!plugin.startGame(args[1])) {
            p.sendMessage(Component.text("Cannot start. Check pos1/pos2/villager/spawn.", NamedTextColor.RED));
        }
    }

    private void stop(Player p) {
        if (!p.hasPermission("dv.admin")) { noPerm(p); return; }
        plugin.stopGame();
        p.sendMessage(Component.text("Stopped.", NamedTextColor.YELLOW));
    }

    private void invite(Player p, String[] args) {
        if (args.length < 2) { p.sendMessage(Component.text("Usage: /dv invite <player>", NamedTextColor.RED)); return; }
        if (!plugin.isRunning()) { p.sendMessage(Component.text("No game running.", NamedTextColor.RED)); return; }
        Player t = plugin.getServer().getPlayer(args[1]);
        if (t == null) { p.sendMessage(Component.text("Player not online.", NamedTextColor.RED)); return; }
        if (t.equals(p)) { p.sendMessage(Component.text("Cannot invite yourself.", NamedTextColor.RED)); return; }
        t.sendMessage(Component.text(p.getName() + " invited you! Use /dv join", NamedTextColor.GOLD));
        p.sendMessage(Component.text("Invite sent.", NamedTextColor.GREEN));
    }

    private void noPerm(Player p) {
        p.sendMessage(Component.text("No permission.", NamedTextColor.RED));
    }
}
