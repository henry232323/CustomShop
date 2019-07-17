package henry232323.plugin.customshop;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Logger;



public class CustomShop extends JavaPlugin {

    private static final Logger log = Logger.getLogger("Minecraft");
    private Economy econ = null;
    private Permission perms = null;

    public ArrayList<Shop> shops;

    public void save(Object o, File f) {
        try {
            if (!f.exists()) {
                f.createNewFile();
            }

            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(f));
            oos.writeObject(o);
            oos.flush();
            oos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public Object load(File f) {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            Object result = ois.readObject();
            ois.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public void onDisable() {
        log.info(String.format("[%s] Disabled Version %s", getDescription().getName(), getDescription().getVersion()));
        save(shops, new File(getDataFolder(), "shops.dat"));
    }

    @Override
    public void onEnable() {
        File dir = getDataFolder();

        if (!dir.exists()) {
            log.info(String.format("Data directory %s for plugin %s does not exist, creating new directory", dir.getName(), getDescription().getName()));
            if (!dir.mkdir()) {
                log.severe(String.format("Failed to create directory %s", dir.getName()));
            }
        }
        shops = (ArrayList<Shop>) load(new File(getDataFolder(), "shops.dat"));
        if (shops == null) {
            shops = new ArrayList<>();
        }

        for (Shop shop : shops) {
            shop.setPlugin(this);
            shop.getPosition().getBlock().setMetadata("shop", new FixedMetadataValue(this, shop));
        }


        if (!setupEconomy()) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
        }
        setupPermissions();
        getServer().getPluginManager().registerEvents(new ClickListener(this), this);
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions() {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = rsp.getProvider();
        return perms != null;
    }

    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        return false;
    }

    public Economy getEconomy() {
        return econ;
    }

    public Permission getPermissions() {
        return perms;
    }

}
