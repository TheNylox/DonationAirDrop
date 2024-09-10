package com.thenylox.DonationAirDrop.Commands;

import com.thenylox.DonationAirDrop.Functions.PManager;
import com.thenylox.DonationAirDrop.DonationAirDrop;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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

        if(args.length > 0){
            switch(args[0]){
                case "getamount":
                    Player player = (Player) sender;
                    break;
                default:
                    double amount = Double.parseDouble(args[0]);
                    pManager.startDrop(amount);
                    break;

            }
        }
        return false;
    }
}
