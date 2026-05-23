package com.imitatorgame;

import com.imitatorgame.command.ImitatorGameCommand;
import com.imitatorgame.command.ReportCommand;
import com.imitatorgame.command.VoteCommand;
import com.imitatorgame.config.ConfigManager;
import com.imitatorgame.game.GameManager;
import com.imitatorgame.listener.*;
import com.imitatorgame.map.LobbyManager;
import com.imitatorgame.util.Constants;
import org.bukkit.plugin.java.JavaPlugin;

public class ImitatorGamePlugin extends JavaPlugin {

    private ConfigManager configManager;
    private GameManager gameManager;
    private LobbyManager lobbyManager;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        configManager.load();

        lobbyManager = new LobbyManager(this);
        lobbyManager.initLobby();

        gameManager = new GameManager(this);

        registerListeners();
        registerCommands();

        getLogger().info(Constants.PLUGIN_NAME + " v1.0.0 enabled");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.cleanup();
        }
        if (lobbyManager != null) {
            lobbyManager.cleanup();
        }
        getLogger().info(Constants.PLUGIN_NAME + " disabled");
    }

    private void registerListeners() {
        getServer().getPluginManager().registerEvents(new PlayerConnectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerMoveListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new DimensionGuardListener(this), this);
    }

    private void registerCommands() {
        ImitatorGameCommand igCmd = new ImitatorGameCommand(this);
        getCommand("imitatorgame").setExecutor(igCmd);

        VoteCommand voteCmd = new VoteCommand(this);
        getCommand("vote").setExecutor(voteCmd);

        ReportCommand reportCmd = new ReportCommand(this);
        getCommand("report").setExecutor(reportCmd);

        getCommand("radio").setExecutor(new com.imitatorgame.command.RadioCommand(this));
    }

    public ConfigManager getConfigManager() { return configManager; }
    public GameManager getGameManager() { return gameManager; }
    public LobbyManager getLobbyManager() { return lobbyManager; }
}
