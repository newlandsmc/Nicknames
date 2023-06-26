package com.superminecraftservers.nicknames.storage.impl;

import com.superminecraftservers.nicknames.Nicknames;
import com.superminecraftservers.nicknames.storage.StorageProvider;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class SQLStorageProvider implements StorageProvider {
    private String table = "nicknames";
    private static File sqlConfigFile;

    private HikariDataSource dataSource;

    @Override
    public void init(Nicknames plugin) {
        sqlConfigFile = new File(plugin.getDataFolder(), "sql.yml");
        if (!sqlConfigFile.exists()) {
            plugin.getLogger().info("SQL config file not found, creating...");
            plugin.saveResource("sql.yml", false);
            plugin.getLogger().info("Done creating SQL config file");
        }
        plugin.getLogger().info("Initializing SQL storage provider");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(sqlConfigFile);
        String url = config.getString("url");
        String user = config.getString("user");
        String password = config.getString("password");
        String database = config.getString("database");
        int port = config.getInt("port", 3306);
        String driver = config.getString("driver-class", "com.mysql.jdbc.Driver");
        table = config.getString("table", "nicknames");
        try {
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setDriverClassName(driver);
            hikariConfig.setJdbcUrl(
                    "jdbc:mysql://" +
                            url +
                            ":" +
                            port +
                            "/" +
                            database
            );
            hikariConfig.addDataSourceProperty("serverName", url);
            hikariConfig.addDataSourceProperty("port", port);
            hikariConfig.addDataSourceProperty("databaseName", database);
            hikariConfig.addDataSourceProperty("user", user);
            hikariConfig.addDataSourceProperty("password", password);

            hikariConfig.setConnectionTimeout(config.getLong("connection-timeout", 30000));
            hikariConfig.setIdleTimeout(config.getLong("idle-timeout", 600000));
            hikariConfig.setMaxLifetime(config.getLong("max-lifetime", 1800000));

            dataSource = new HikariDataSource(hikariConfig);

            // contents are bytes
            String sql = "CREATE TABLE IF NOT EXISTS " + table + "(UUID varchar(255), nickname varchar(255))";
            PreparedStatement statement = dataSource.getConnection().prepareStatement(sql);
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void disable(Nicknames plugin) {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    @Override
    public void save(UUID uuid, String nickname) {
        String selectSQL = "SELECT * FROM " + table + " WHERE UUID = ?";
        try {
            Connection connection = dataSource.getConnection();
            PreparedStatement selectStatement = connection.prepareStatement(selectSQL);
            selectStatement.setString(1, uuid.toString());
            selectStatement.execute();
            if (selectStatement.getResultSet().next()) {
                String updateSQL = "UPDATE " + table + " SET nickname = ? WHERE UUID = ?";
                PreparedStatement updateStatement = connection.prepareStatement(updateSQL);
                updateStatement.setString(1, nickname);
                updateStatement.setString(2, uuid.toString());
                updateStatement.execute();
            } else {
                String insertSQL = "INSERT INTO " + table + " (UUID, nickname) VALUES (?, ?)";
                PreparedStatement insertStatement = connection.prepareStatement(insertSQL);
                insertStatement.setString(1, uuid.toString());
                insertStatement.setString(2, nickname);
                insertStatement.execute();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String load(UUID uuid) {
        try {
            String sql = "SELECT nickname FROM " + table + " WHERE UUID = ?";
            PreparedStatement statement = dataSource.getConnection().prepareStatement(sql);
            statement.setString(1, uuid.toString());
            statement.execute();
            if (!statement.getResultSet().next()) {
                return null;
            }
            return statement.getResultSet().getString("nickname");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(UUID uuid) {
        try {
            String sql = "DELETE FROM " + table + " WHERE UUID = ?";
            PreparedStatement statement = dataSource.getConnection().prepareStatement(sql);
            statement.setString(1, uuid.toString());
            statement.execute();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
