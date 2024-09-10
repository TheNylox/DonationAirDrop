package com.thenylox.DonationAirDrop;

import com.thenylox.DonationAirDrop.Commands.Dd;
import com.thenylox.DonationAirDrop.Functions.PManager;
import com.thenylox.DonationAirDrop.Placeholders.PlaceholderAPIExtensions;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public final class DonationAirDrop extends JavaPlugin {
    private PManager pManaqer;

    @Override
    public void onEnable() {
        Logger log = getLogger();

        // Codici colore ANSI
        String reset = "\u001B[0m";
        String cyan = "\u001B[36m";
        String brightGreen = "\u001B[92m";
        String yellow = "\u001B[33m";

        //Intestazione
        log.info(yellow+        "==========================================================================="+reset);
        log.info(brightGreen+   "Donation Air Drop"+reset+brightGreen+" v0.3                                          "+reset+cyan+"by TheNylox");
        log.info(yellow+        "==========================================================================="+reset);

        //Config Loader
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        //File di config
        File configLoader = new File(getDataFolder(), "config.yml");
        if (!configLoader.exists()) {
            saveResource("config.yml", false);
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configLoader);
        log.info(brightGreen +"×"+cyan+" Config File Loaded"+reset);

        //Lang
        File langDir = new File(getDataFolder(), "lang");
        if (!langDir.exists()) {
            langDir.mkdir();
        }

        String langType = config.getString("lang")+".yml";
        File langLoader = new File(langDir, langType);
        if (!langLoader.exists()) {
            saveResource("lang/"+langType, false);
        }
        YamlConfiguration langFile = YamlConfiguration.loadConfiguration(langLoader);
        log.info(brightGreen +"×"+cyan+" Lang Loaded"+reset);

        //Flat db
        File dbLoader = new File(getDataFolder(), "db.yml");
        if (!dbLoader.exists()) {
            saveResource("db.yml", false);
        }
        YamlConfiguration db = YamlConfiguration.loadConfiguration(dbLoader);
        log.info(brightGreen +"×"+cyan+" Flat DB Loaded"+reset);

        //Istanza del plugin
        pManaqer = new PManager(config, db, dbLoader, this, langFile);
        pManaqer.loadSystem();

        //Commands
        this.getCommand("dd").setExecutor(new Dd(this));

        //Placeholder
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderAPIExtensions(this).register();
        }
        log.info(brightGreen +"×"+cyan+" Custom Placeholders loaded"+reset);

        log.info(brightGreen+"PLUGIN ENABLED");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public PManager getPManaqer(){
        return pManaqer;
    }
}
