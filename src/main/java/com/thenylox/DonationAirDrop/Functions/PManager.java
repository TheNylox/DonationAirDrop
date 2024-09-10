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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.bukkit.Bukkit.getLogger;

public class PManager {
    private final YamlConfiguration config;
    private final DonationAirDrop plugin;
    private Map<Double, Integer> rangeMap = new HashMap<>();
    private Map<Integer, Location> chestLocations = new HashMap<>();
    private Cuboid spawnCuboid;
    private boolean isStackDrop;
    private final YamlConfiguration db;
    private final File dbFile;
    private double loadedAmount;
    private String lastRefresh;
    private int checkValue;
    private char checkType;
    private boolean debug;
    private YamlConfiguration lt;
    private String title;

    public PManager(YamlConfiguration config,YamlConfiguration db, File dbFile, DonationAirDrop plugin, YamlConfiguration lt) {
        this.config = config;
        this.plugin = plugin;
        this.db = db;
        this.dbFile = dbFile;
        this.lt = lt;
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
        loadedAmount = Double.valueOf(db.getString("amount"));

        //Carico data ultimo reset e relativi dati
        lastRefresh = db.getString("time");
        checkType = config.getString("checkType").charAt(0);
        checkValue = config.getInt("checkValue");
        debug = config.getBoolean("debugMode");
        title = lt.getString("title");
    }


    //Logic
    private void loadRanges() {
        Map<String, Object> ranges = config.getConfigurationSection("ranges").getValues(false);

        if (ranges == null) {
            error("noranges");
            return;
        }

        for (Map.Entry<String, Object> entry : ranges.entrySet()) {
            String keyString = entry.getKey();
            String range = (String) entry.getValue();

            if(debug) message("range_process",range,'w');

            String[] rangeParts = range.split("-");

            if(debug) message("range_parts", Arrays.toString(rangeParts),'w');

            if (rangeParts.length != 2) {
                error("range_invalidvalue", range);
                continue;
            }

            try {
                double start = Double.parseDouble(rangeParts[0].trim());
                double end = Double.parseDouble(rangeParts[1].trim());
                Integer key;

                try {
                    key = Integer.parseInt(keyString);
                } catch (NumberFormatException e) {
                    error("range_invalidkey", keyString);
                    continue;
                }

                if(debug) message("range_parsed",start + " to " + end + " with key: " + key,'w');

                for (double i = start; i <= end; i += 0.01) {
                    rangeMap.put(i, key);
                }
            } catch (NumberFormatException e) {
                message("range_invalidnumber", range,'w');
            }
        }
    }
    private void loadCuboid() {
        final World world = Bukkit.getWorld(config.getString("world"));

        if(world == null)
            error("world_notfound",config.getString("world"));

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
                    error("general_step","1");
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
        // Filtra e ordina le voci in ordine crescente per chiave
        return rangeMap.entrySet().stream()
                .filter(entry -> entry.getKey() <= number) // Filtra solo le voci che sono <= al numero
                .sorted(Map.Entry.comparingByKey()) // Ordina le voci per chiave in ordine crescente
                .reduce((first, second) -> second) // Prendi l'ultima voce che è <= al numero
                .map(Map.Entry::getValue) // Ottieni il valore dell'ultima voce
                .orElse(null); // Restituisce null se nessuna voce soddisfa il filtro
    }


    public boolean hasTimePassed() {
        // Formato della data
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // Recupera il tempo attuale
        LocalDateTime actualTime = LocalDateTime.now();
        LocalDate actualDate = actualTime.toLocalDate(); // Solo data, senza ora

        // Parse della data di ultimo aggiornamento
        LocalDate lastRefreshDate = LocalDate.parse(lastRefresh, formatter);

        // Incrementa la data di ultimo aggiornamento
        LocalDate updatedDate = lastRefreshDate;
        if (checkType == 'd') {
            updatedDate = updatedDate.plusDays(checkValue);
        } else if (checkType == 'm') {
            updatedDate = updatedDate.plusMonths(checkValue);
        } else {
            error("checktype_invalid");
        }

        // Confronta la data attuale con la data aggiornata
        boolean isPassed = actualDate.isAfter(updatedDate) || actualDate.isEqual(updatedDate);

        return isPassed;
    }
    public void resetTimeAndValue() {
        // Crea un formatter per il formato desiderato
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

        // Ottieni il tempo attuale e formatta come stringa
        LocalDateTime newTime = LocalDateTime.now();
        lastRefresh = newTime.format(formatter);

        // Imposta i valori nel database
        db.set("amount", "0");
        db.set("time", lastRefresh);

        // Salva le modifiche al file
        try {
            db.save(this.dbFile);
        } catch (IOException e) {
            error("file_cannotsave");
            throw new RuntimeException("Errore durante il salvataggio del file di configurazione", e);
        }

        loadedAmount = 0;
    }
    private void updateAmount(double amount){
        loadedAmount = loadedAmount + amount;
        db.set("amount",loadedAmount);

        try {
            db.save(this.dbFile);
        } catch (IOException e) {
            error("file_cannotsave");
            throw new RuntimeException("Errore durante il salvataggio del file di configurazione", e);
        }
    }

    //Get
    public String getAmount(){ return String.valueOf(loadedAmount);}

    //Log Builder
    private void message(String messageName, Character type){
    if(type == null)
        type = 'w';

        switch(type){
            case 'i':
                Bukkit.getLogger().info(title+lt.getString(messageName)+" ");
                break;
            case 'w':
                Bukkit.getLogger().warning(title+lt.getString(messageName)+" ");
                break;
            case 'e':
                Bukkit.getLogger().severe(title+lt.getString(messageName)+" ");
                break;
        }

    }
    private void message(String messageName,String other, Character type){
        if(type == null)
            type = 'w';

        switch(type){
            case 'i':
                Bukkit.getLogger().info(title+lt.getString(messageName)+" ");
                break;
            case 'w':
                Bukkit.getLogger().warning(title+lt.getString(messageName)+" ");
                break;
            case 'e':
                Bukkit.getLogger().severe(title+lt.getString(messageName)+" ");
                break;
        }

    }
    private void error(String messageName,String other){
        Bukkit.getLogger().severe(title+lt.getString(messageName)+" "+other);
    }
    private void error(String messageName){
        Bukkit.getLogger().severe(title+lt.getString(messageName)+" ");
    }

    //Executor
    public void startDrop(double prezzoPagato) {

        //Controllo se devo refreshare
        if(hasTimePassed())
            resetTimeAndValue();

        int giveType = getGiveType(prezzoPagato);

        if (giveType == 0) {
            error("give_notypefound");
            return;
        }

        Location chestLocation = getChestLocation(giveType);

        if (chestLocation == null) {
            error("give_nochestfound", String.valueOf(giveType));
            return;
        }

        Block block = chestLocation.getBlock();
        if (!(block.getState() instanceof Chest)) {
            error("give_blockisnotchest");
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