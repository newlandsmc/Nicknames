package com.superminecraftservers.nicknames.placeholder;

import com.superminecraftservers.nicknames.Nicknames;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class NicknameExpansion extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "nickname";
    }

    @Override
    public @NotNull String getAuthor() {
        return "nicknames";
    }

    @SuppressWarnings("deprecation") // Nicknames has a plugin.yml
    @Override
    public @NotNull String getVersion() {
        return Nicknames.getInstance().getDescription().getVersion();
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;
        // if (params.equalsIgnoreCase("nickname"))
        String nick = Nicknames.getInstance().getNickname(player.getUniqueId());
        if (nick == null) return player.isOnline() ? player.getPlayer().getDisplayName() : player.getName();
        // return net.md_5.bungee.api.ChatColor.translateAlternateColorCodes('&', nick);
        return nick;
        // return null;
        // return super.onRequest(player, params);
    }
}
