package com.superminecraftservers.nicknames.storage;

import com.superminecraftservers.nicknames.Nicknames;

import java.util.UUID;

public interface StorageProvider {
    void init(Nicknames plugin);

    void disable(Nicknames plugin);

    void save(UUID uuid, String nickname);

    void delete(UUID uuid);

    String load(UUID uuid);
}
