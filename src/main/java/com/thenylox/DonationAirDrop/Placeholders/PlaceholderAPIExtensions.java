package com.thenylox.DonationAirDrop.Placeholders;

import com.thenylox.DonationAirDrop.DonationAirDrop;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class PlaceholderAPIExtensions extends PlaceholderExpansion {

    private final DonationAirDrop plugin;

    public PlaceholderAPIExtensions(DonationAirDrop plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zdd";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }
    @Override
    public String onRequest(OfflinePlayer player, String identifier) {
        switch(identifier){
            case "amount":
                return plugin.getPManaqer().getAmount();
            default:
                return null;
        }
    }
}
