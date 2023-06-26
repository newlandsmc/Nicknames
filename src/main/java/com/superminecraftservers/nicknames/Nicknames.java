package com.superminecraftservers.nicknames;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.superminecraftservers.nicknames.placeholder.NicknameExpansion;
import com.superminecraftservers.nicknames.storage.StorageProvider;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

public final class Nicknames extends JavaPlugin {
    private static Nicknames instance;

    public static Nicknames getInstance() {
        return instance;
    }

    private StorageProvider storageProvider;
    private boolean useCache = true;
    private Pattern allowedPattern = null;
    private static final Object NULL = new Object();
    private CacheLoader<UUID, Object> cacheLoader = new CacheLoader<>() {
        @Override
        public Object load(@NotNull UUID key) {
            String s = storageProvider.load(key);
            if (s == null) return NULL;
            return s;
        }
    };
    private LoadingCache<UUID, Object> loadingCache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterAccess(20, java.util.concurrent.TimeUnit.MINUTES)
            .build(cacheLoader);

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        try {
            File dataFolder = getDataFolder();
            if (!dataFolder.exists()) {
                if (!dataFolder.mkdirs()) {
                    throw new RuntimeException("Failed to create data folder");
                }
            }
            saveDefaultConfig();
            try {
                String storageProvider = getConfig().getString("storage-provider");
                getLogger().info("Initializing storage provider " + storageProvider);
                Class<?> storageProviderClass = Class.forName("com.superminecraftservers.nicknames.storage.impl." + storageProvider);
                this.storageProvider = (StorageProvider) storageProviderClass.newInstance();
            } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
                throw new RuntimeException(e);
            }

            this.storageProvider.init(this);

            getCommand("nickname").setExecutor(this);
            getCommand("nickname").setTabCompleter(this);

            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new NicknameExpansion().register();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public String getNickname(UUID uuid) {
        if (useCache) {
            Object o = loadingCache.getUnchecked(uuid);
            if (o == NULL) return null;
            return (String) o;
        }
        return storageProvider.load(uuid);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        useCache = getConfig().getBoolean("cache.enabled", true);
        String regex = getConfig().getString("allowed-regex", "");
        if (regex.isEmpty()) {
            allowedPattern = null;
        } else {
            allowedPattern = Pattern.compile(regex);
        }
    }

    private static final Pattern UUID_REGEX = Pattern.compile("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}");

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        Runnable sendUsage = () -> {
            sender.sendRichMessage("<red>Usage:");
            sender.sendRichMessage("<red> - /nickname set <player> <nickname>");
            sender.sendRichMessage("<red> - /nickname reset <player>");
            sender.sendRichMessage("<red> - /nickname get <player>");
            sender.sendRichMessage("<red> - /nickname reload");
        };
        if (args.length == 0) {
            sendUsage.run();
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) {
                    sendUsage.run();
                    return true;
                }
                String playerName = args[1];
                OfflinePlayer op;
                if (UUID_REGEX.matcher(playerName).matches()) {
                    op = Bukkit.getOfflinePlayer(UUID.fromString(playerName));
                } else {
                    op = Bukkit.getOfflinePlayer(playerName);
                }
                String[] nicknameParts = new String[args.length - 2];
                System.arraycopy(args, 2, nicknameParts, 0, nicknameParts.length);
                String nickname = String.join(" ", nicknameParts);
                if (!sender.hasPermission("nicknames.regex.bypass")) {
                    if (allowedPattern != null && !allowedPattern.matcher(nickname).matches()) {
                        sender.sendRichMessage("<red>That nickname is not allowed.");
                        return true;
                    }
                }
                loadingCache.put(op.getUniqueId(), nickname);
                storageProvider.save(op.getUniqueId(), nickname);
                // sender.sendMessage(net.md_5.bungee.api.ChatColor.GREEN + "Set nickname of " + op.getName() + " to " + net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', nickname));
                sender.sendRichMessage("<green>Set nickname of <white>" + op.getName() + "<green> to <white>" + nickname);
                return true;
            }
            case "reset" -> {
                if (args.length != 2) {
                    sendUsage.run();
                    return true;
                }
                loadingCache.invalidate(UUID.fromString(args[1]));
                storageProvider.delete(UUID.fromString(args[1]));
                return true;
            }
            case "get" -> {
                if (args.length != 2) {
                    sendUsage.run();
                    return true;
                }
                String playerName = args[1];
                OfflinePlayer op;
                if (UUID_REGEX.matcher(playerName).matches()) {
                    op = Bukkit.getOfflinePlayer(UUID.fromString(playerName));
                } else {
                    op = Bukkit.getOfflinePlayer(playerName);
                }
                String nickname = getNickname(op.getUniqueId());
                if (nickname == null) {
                    sender.sendRichMessage("<red>That player does not have a nickname.");
                } else {
                    sender.sendRichMessage("<green>Nickname of <white>" + args[1] + "<green> is <white>" + nickname);
                }
                return true;
            }
            case "reload" -> {
                reloadConfig();
                sender.sendRichMessage("<green>Reloaded config.");
                return true;
            }
        }
        sendUsage.run();
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return Arrays.asList("set", "reset", "get", "reload");
        }
        return null;
    }

    @Override
    public void onDisable() {
        if (storageProvider != null) storageProvider.disable(this);
    }
}
