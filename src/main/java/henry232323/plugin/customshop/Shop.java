package henry232323.plugin.customshop;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.io.Serializable;
import java.util.UUID;

public class Shop implements Serializable {
    private UUID ownerID;
    private int x, y, z;
    private String worldName;

    int buyNumber = 0;
    int sellNumber = 0;
    String buyItem;
    String sellItem;
    boolean buyCurrency = false;
    boolean sellCurrency = false;

    transient CustomShop plugin;


    public Shop(OfflinePlayer owner, Location pos, int bNumber, int sNumber, String bItem, String sItem) {
        this.ownerID = owner.getUniqueId();
        this.worldName = pos.getWorld().getName();
        this.x = pos.getBlockX();
        this.y = pos.getBlockY();
        this.z = pos.getBlockZ();

        if (bItem.equals("")) {
            buyCurrency = true;
        }
        if (sItem.equals("")) {
            sellCurrency = true;
        }


        buyItem = bItem;
        sellItem = sItem;
        buyNumber = bNumber;
        sellNumber = sNumber;
    }

    public Location getPosition() {
        World world = plugin.getServer().getWorld(worldName);
        return new Location(
                world,
                x,
                y,
                z
        );
    }

    public Location getStoragePosition() {
        return getPosition().subtract(0, 1, 0);
    }

    public OfflinePlayer getOwner() {
        return plugin.getServer().getOfflinePlayer(ownerID);
    }

    public void setPlugin(CustomShop cshop) {
        plugin = cshop;
    }

}
