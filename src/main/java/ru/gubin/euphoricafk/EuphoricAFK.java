package ru.gubin.euphoricafk;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.ChatPaginator;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EuphoricAFK extends JavaPlugin implements Listener {

    private final Map<UUID, Long> afkPlayers = new HashMap<>();
    private final Map<UUID, Boolean> afkNotified = new HashMap<>();
    private long afkWarningTime;
    private long afkKickTime;
    private String afkWarningMessage;
    private String afkKickMessage;
    private String reloadMessage;

    @Override
    public void onEnable() {
        createPluginFolder();
        saveDefaultConfig();
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, this);
        new AFKCheckTask().runTaskTimer(this, 0, 20);
    }

    @Override
    public void onDisable() {
        afkPlayers.clear();
        afkNotified.clear();
    }

    private void createPluginFolder() {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            pluginFolder.mkdirs();
        }
    }

    @Override
    public void saveDefaultConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        afkWarningTime = config.getLong("afkWarningTime", 60) * 1000;
        afkKickTime = config.getLong("afkKickTime", 120) * 1000;
        afkWarningMessage = ChatColor.translateAlternateColorCodes('&', config.getString("afkWarningMessage", "Вы стоите в AFK. Через 2 минуты Вас кикнут."));
        afkKickMessage = ChatColor.translateAlternateColorCodes('&', config.getString("afkKickMessage", "Вы стояли в AFK"));
        reloadMessage = ChatColor.translateAlternateColorCodes('&', config.getString("reloadMessage", "Конфигурация плагина EuphoricAFK перезагружена."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("euphoricafk")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("euphoricafk.reload")) {
                    reloadConfig();
                    loadConfig();
                    sender.sendMessage(reloadMessage);
                    return true;
                } else {
                    sender.sendMessage(ChatColor.RED + "У вас нет прав для выполнения этой команды.");
                    return true;
                }
            } else if (args.length > 0 && args[0].equalsIgnoreCase("version")) {
                sender.sendMessage(ChatColor.RESET + "");
                sender.sendMessage(ChatColor.WHITE + "Версия плагина: " + ChatColor.UNDERLINE + ChatColor.GOLD + "1.0 STABLE" + ChatColor.RESET);
                sender.sendMessage(ChatColor.RESET + "");
                return true;
            } else if (args.length > 0 && args[0].equalsIgnoreCase("author")) {
                sender.sendMessage(ChatColor.RESET + "");
                sender.sendMessage(ChatColor.WHITE + "Автор: " + ChatColor.UNDERLINE + ChatColor.GREEN + "Gubin" + ChatColor.RESET);
                sender.sendMessage(ChatColor.WHITE + "Дискорд автора: " + ChatColor.UNDERLINE + ChatColor.YELLOW + "@gu.b" + ChatColor.RESET);
                sender.sendMessage(ChatColor.WHITE + "Дискорд сервер: " + ChatColor.UNDERLINE + ChatColor.AQUA + "https://discord.gg/B6QCGttPtq" + ChatColor.RESET);
                sender.sendMessage(ChatColor.RESET + "");
                return true;
            }
        }
        return false;
    }

    @org.bukkit.event.EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        afkPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        afkNotified.put(player.getUniqueId(), false);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        afkPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        afkNotified.put(player.getUniqueId(), false);
    }

    @org.bukkit.event.EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        afkPlayers.put(player.getUniqueId(), System.currentTimeMillis());
        afkNotified.put(player.getUniqueId(), false);
    }

    private class AFKCheckTask extends BukkitRunnable {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            for (Player player : Bukkit.getOnlinePlayers()) {
                UUID playerId = player.getUniqueId();
                if (player.hasPermission("euphoricafk.bypass")) {
                    continue;
                }
                if (!afkPlayers.containsKey(playerId)) {
                    afkPlayers.put(playerId, currentTime);
                    afkNotified.put(playerId, false);
                    continue;
                }

                long lastMoveTime = afkPlayers.get(playerId);
                if (currentTime - lastMoveTime >= afkWarningTime && currentTime - lastMoveTime < afkKickTime) {
                    if (!afkNotified.get(playerId)) {
                        player.sendMessage(afkWarningMessage);
                        afkNotified.put(playerId, true);
                    }
                } else if (currentTime - lastMoveTime >= afkKickTime) {
                    player.kickPlayer(afkKickMessage);
                    afkPlayers.remove(playerId);
                    afkNotified.remove(playerId);
                }
            }
        }
    }
}
