package com.thenylox.DonationAirDrop.Commands;

import com.thenylox.DonationAirDrop.Functions.PManager;
import com.thenylox.DonationAirDrop.DonationAirDrop;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class Dd implements CommandExecutor {
    private final DonationAirDrop plugin;

    //Istanza del Main
    public Dd(DonationAirDrop plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        PManager pManager = plugin.getPManaqer();

        if (args.length > 0) {
            switch (args[0]) {
                case "getamount":
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage("Actual Amount: " + pManager.getAmount());
                    }
                    Bukkit.getLogger().info("Actual Amount: "+pManager.getAmount());
                    break;

                case "reload":
                    Bukkit.getPluginManager().disablePlugin(plugin);
                    Bukkit.getPluginManager().enablePlugin(plugin);
                    plugin.getLogger().info("\u001B[92m DonationAirdrop RELOADED");
                    break;

                case "start":
                    if (args.length > 1) {
                        double value = -1;
                        try {
                            value = Double.parseDouble(args[1]);
                        } catch (NumberFormatException e) {
                            if (sender instanceof Player) {
                                Player player = (Player) sender;
                                player.sendMessage("Errore nel comando, l'argomento non può essere convertito in double. Arg: " + args[1]);
                            }
                            Bukkit.getLogger().warning("Errore nel comando, l'argomento non può essere convertito in double. Arg: " + args[1]);
                        }

                        if (value != -1) {
                            pManager.startDrop(value);
                        }
                        break;
                    }
                default:
                    if (sender instanceof Player) {
                        Player player = (Player) sender;
                        player.sendMessage(ChatColor.RED + "[DonationAirdrop] Unknown argument");
                    } else {
                        Bukkit.getLogger().warning("[DonationAirDrop] Unknown argument");
                    }
                    break;

            }
        }
        return false;
    }
}
