package com.thenylox.DonationAirDrop.Functions;

import com.thenylox.DonationAirDrop.Utils.Cuboid;
import com.thenylox.DonationAirDrop.DonationAirDrop;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.bukkit.Bukkit.getLogger;

public class PManager {
    private final YamlConfiguration config;
    private final DonationAirDrop plugin;
    private Map<Integer, Integer> rangeMap = new HashMap<>();
    private Map<Integer, Location> chestLocations = new HashMap<>();
    private Cuboid spawnCuboid;
    private boolean isStackDrop;
    private final YamlConfiguration db;
    private final File dbFile;
    private double loadedAmount;
    private LocalDate lastRefresh;
    private int checkValue;
    private char checkType;

    public PManager(YamlConfiguration config,YamlConfiguration db, File dbFile, DonationAirDrop plugin) {
        this.config = config;
        this.plugin = plugin;
        this.db = db;
        this.dbFile = dbFile;
    }

    public void loadSystem() {
        // Carico region
        loadCuboid();

        // Carico ranges
        loadRanges();

        // Carico chests
        loadChestLocations();

        // Check se il drop va a stack
        isStackDrop = config.getBoolean("isStackDrop");

        // Carico ammontare in memoria
        loadedAmount = db.getDouble("amount");

        //Carico data ultimo reset e relativi dati
        String retrievedTime = db.getString("time");
        lastRefresh = LocalDateTime.parse(retrievedTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();

        checkType = config.getString("checkType").charAt(0);
        checkValue = config.getInt("checkValue");
    }


    //Logic
    private void loadRanges() {
        Map<String, Object> ranges = config.getConfigurationSection("ranges").getValues(false);

        for (Map.Entry<String, Object> entry : ranges.entrySet()) {
            String[] rangeParts = entry.getKey().split("-");
            int start = Integer.parseInt(rangeParts[0]);
            int end = Integer.parseInt(rangeParts[1]);
            Integer value = (Integer) entry.getValue();

            for (int i = start; i <= end; i++) {
                rangeMap.put(i, value);
            }
        }
    }
    private void loadCuboid() {
        final World world = Bukkit.getWorld(config.getString("world"));

        double x1 = config.getDouble("x1");
        double y1 = config.getDouble("y1");
        double z1 = config.getDouble("z1");

        double x2 = config.getDouble("x2");
        double y2 = config.getDouble("y2");
        double z2 = config.getDouble("z2");

        spawnCuboid = new Cuboid(
                new Location(world, x1, y1, z1),
                new Location(world, x2, y2, z2)
        );
    }
    private void loadChestLocations() {
        if (config.contains("chests")) {
            for (String key : config.getConfigurationSection("chests").getKeys(false)) {
                int id = Integer.parseInt(key);
                String worldName = config.getString("chests." + key + ".world");
                int x = config.getInt("chests." + key + ".x");
                int y = config.getInt("chests." + key + ".y");
                int z = config.getInt("chests." + key + ".z");

                if (Bukkit.getServer().getWorld(worldName) != null) {
                    Location location = new Location(Bukkit.getServer().getWorld(worldName), x, y, z);
                    chestLocations.put(id, location);
                } else {
                    getLogger().warning("[zDonationAirDrop] World " + worldName + " not found for chest ID " + id);
                }
            }
        }
    }
    private Location getRandomLocationInCuboid(Cuboid cuboid) {
        Random random = new Random();
        double x = random.nextDouble() * (cuboid.getUpperX() - cuboid.getLowerX()) + cuboid.getLowerX();
        double y = random.nextDouble() * (cuboid.getUpperY() - cuboid.getLowerY()) + cuboid.getLowerY();
        double z = random.nextDouble() * (cuboid.getUpperZ() - cuboid.getLowerZ()) + cuboid.getLowerZ();
        return new Location(cuboid.getWorld(), x, y, z);
    }
    private Location getChestLocation(int id) {
        return chestLocations.get(id);
    }
    private Integer getGiveType(double number) {
        return rangeMap.get(number);
    }
    public boolean hasTimePassed() {
        LocalDate currentDate = LocalDate.now();

        if (checkType == 'd') {
            LocalDate targetDate = lastRefresh.plusDays(checkValue);
            return currentDate.isAfter(targetDate) || currentDate.isEqual(targetDate);
        } else if (checkType == 'm') {
            LocalDate targetDate = lastRefresh.plusMonths(checkValue);
            return currentDate.isAfter(targetDate) || currentDate.isEqual(targetDate);
        }
        return false;
    }
    public void resetTimeAndValue(){
        LocalDate newTime = LocalDate.now();
        lastRefresh = newTime;

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        db.set("amount",0);
        db.set("time", newTime.format(formatter));

        try {
            db.save(this.dbFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }
    private void updateAmount(double amount){
        loadedAmount = loadedAmount + amount;
        db.set("amount",loadedAmount);

        try {
            db.save(this.dbFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //Get
    public String getAmount(){ return String.valueOf(loadedAmount);}

    //Executor
    public void startDrop(double prezzoPagato) {

        //Controllo se devo refreshare
        if(hasTimePassed())
            resetTimeAndValue();

        int giveType = getGiveType(prezzoPagato);

        if (giveType == 0) {
            Bukkit.getLogger().warning("[zDonationAirDrop] No give type found for price " + prezzoPagato);
            return;
        }

        Location chestLocation = getChestLocation(giveType);

        if (chestLocation == null) {
            Bukkit.getLogger().warning("[zDonationAirDrop] No chest location found for ID " + giveType);
            return;
        }

        Block block = chestLocation.getBlock();
        if (!(block.getState() instanceof Chest)) {
            Bukkit.getLogger().warning("[zDonationAirDrop] Block at " + chestLocation + " is not a chest.");
            return;
        }

        Chest chest = (Chest) block.getState();
        Inventory inventory = chest.getInventory();
        ItemStack[] items = inventory.getContents();

        long delay = config.getInt("dropDelay") * 20L;


        if (isStackDrop) {
            new BukkitRunnable() {
                private int index = 0;

                @Override
                public void run() {
                    if (index < items.length) {
                        ItemStack item = items[index];
                        if (item != null && item.getType() != Material.AIR) {
                            Location dropLocation = getRandomLocationInCuboid(spawnCuboid); // Usa una posizione casuale nel cuboid
                            Item droppedItem = chestLocation.getWorld().dropItem(dropLocation, item.clone());
                        }
                        index++;
                    } else {
                        cancel();
                    }
                }
            }.runTaskTimer(plugin, 0L, delay);

        }else {
            new BukkitRunnable() {
                private int index = 0;
                private int stackCount = 0;
                private ItemStack currentItem = null;

                @Override
                public void run() {
                    if (index >= items.length) {
                        cancel();
                        return;
                    }

                    if (currentItem == null || stackCount == currentItem.getAmount()) {
                        currentItem = items[index];
                        index++;
                        stackCount = 0; // Reset dello stack
                    }

                    if (currentItem != null && currentItem.getType() != Material.AIR) {
                        Location dropLocation = getRandomLocationInCuboid(spawnCuboid);

                        ItemStack singleItem = currentItem.clone();
                        singleItem.setAmount(1);

                        Item droppedItem = chestLocation.getWorld().dropItem(dropLocation, singleItem);
                        stackCount++;
                    }
                }
            }.runTaskTimer(plugin, 0L, delay);
        }

        updateAmount(prezzoPagato);
    }
}