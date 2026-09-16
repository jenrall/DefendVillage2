package com.example.dv;

import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class DVListener implements Listener {

    private final DVPlugin plugin;

    public DVListener(DVPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntity() instanceof Zombie) {
            plugin.onZombieDeath(e.getEntity().getUniqueId());
        } else if (e.getEntity() instanceof Villager) {
            plugin.onVillagerDeath(e.getEntity().getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.leaveGame(e.getPlayer());
    }
}
