package henry232323.plugin.customshop;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Sign;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.ChatColor;

import java.io.File;
import java.util.List;

public class ClickListener implements Listener {

    CustomShop plugin;

    public ClickListener(CustomShop plugin) {
        super();
        this.plugin = plugin;
    }

    @EventHandler
    public void onBlockDestroyed(BlockDestroyEvent event) {
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Sign || block.getBlockData() instanceof WallSign)) {
            return;
        }

        if (block.getBlockData() instanceof Sign || block.getBlockData() instanceof WallSign) {
            if (block.hasMetadata("shop")) {
                block.removeMetadata("shop", plugin);
            }
        }
    }

    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        System.out.println(2);
        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }
        System.out.println(3);

        if (block.getBlockData() instanceof Sign || block.getBlockData() instanceof WallSign) {
            System.out.println(4);
            if (block.hasMetadata("shop")) {
                System.out.println(5);
                List<MetadataValue> mdv = block.getMetadata("shop");
                for (MetadataValue val : mdv) {
                    if (val.getOwningPlugin() == plugin) {
                        Shop shop = (Shop) val.value();
                        System.out.println(6);
                        playerShopInteract(event.getPlayer(), shop);
                        System.out.println(7);
                    }
                }
            }
        }

    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {
        Block block = event.getBlock();
        if (!(block.getBlockData() instanceof Sign || block.getBlockData() instanceof WallSign)) {
            return;
        }
        if (!(block.getLocation().subtract(0, 1, 0).getBlock().getBlockData() instanceof Chest)) {
            return;
        }
        if (event.getLines()[0].equals("[Trade]")) {
            try {
                String buyItem = "";
                int buyAmount;
                if (event.getLines()[1].charAt(0) == '$') {
                    buyAmount = Integer.parseInt(event.getLines()[1].substring(1, event.getLines()[1].length()));
                } else {
                    String[] parts = event.getLines()[1].split(" ");
                    if (parts.length > 2) {
                        return;
                    }
                    buyAmount = Integer.parseInt(parts[0]);
                    buyItem = parts[1];
                    if (buyAmount > 64) {
                        return;
                    }
                    Material mat = Material.getMaterial(buyItem);
                    if (mat == null) {
                        return;
                    }
                }

                String sellItem = "";
                int sellAmount;

                if (event.getLines()[2].charAt(0) == '$') {
                    sellAmount = Integer.parseInt(event.getLines()[2].substring(1, event.getLines()[2].length()));
                } else {
                    String[] parts = event.getLines()[3].split(" ");
                    if (parts.length > 2) {
                        return;
                    }
                    sellAmount = Integer.parseInt(parts[0]);
                    if (sellAmount > 64) {
                        return;
                    }
                    sellItem = parts[1];
                    Material mat = Material.getMaterial(sellItem);
                    if (mat == null) {
                        return;
                    }
                }

                org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
                sign.setLine(3, event.getPlayer().getDisplayName());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Successfully created new shop!");

                Shop shop = new Shop(event.getPlayer(), sign.getLocation(), buyAmount, sellAmount, buyItem, sellItem);
                sign.setMetadata("shop", new FixedMetadataValue(plugin, shop));
                plugin.shops.add(shop);
                plugin.save(plugin.shops, new File(plugin.getDataFolder(), "shops.dat"));

            } catch (Exception e) {
                return;
            }
        }

    }

    public void playerShopInteract(Player player, Shop shop) {
        System.out.println("Player shop interact!");
        Block block = shop.getStoragePosition().getBlock();
        org.bukkit.block.Chest chest;
        try {
            chest = (org.bukkit.block.Chest) block.getState();
        } catch (Exception e ) {
            player.sendMessage("This shop is broken! Contact the owner!");
            return;
        }

        Inventory chestInv = chest.getBlockInventory();
        Inventory playerInv = player.getInventory();
        boolean buyerCheck = false;
        boolean sellerCheck = false;
        if (shop.sellItem.equals("")) {
            System.out.println(shop.getOwner());
            System.out.println(pl)
            if (plugin.getEconomy().has(shop.getOwner(), shop.sellNumber)) {
                buyerCheck = true;
            }
        } else if (!chestInv.contains(Material.getMaterial(shop.sellItem), shop.sellNumber)) {
            buyerCheck = true;
        }

        if (shop.buyItem.equals("")) {
            if (plugin.getEconomy().has(player, shop.buyNumber)) {
                sellerCheck = true;
            }
        } else if (!playerInv.contains(Material.getMaterial(shop.buyItem), shop.buyNumber)) {
            sellerCheck = true;
            if (playerInv.firstEmpty() == -1) {
                player.sendMessage(ChatColor.RED + "You do not have any space in your inventory!");
                return;
            }
        }

        if (!buyerCheck) {
            player.sendMessage(ChatColor.RED + "This shop is out of stock!");
            return;
        }
        if (!sellerCheck) {
            player.sendMessage(ChatColor.RED + "You cannot afford to complete this transaction!");
            return;
        }

        if (shop.sellItem.equals("")) {
            plugin.getEconomy().withdrawPlayer(shop.getOwner(), shop.sellNumber);
            plugin.getEconomy().depositPlayer(player, shop.sellNumber);

            player.sendMessage(ChatColor.GREEN + String.format("%s was deposited into your account.", shop.sellNumber));
            if (shop.getOwner().isOnline()) {
                plugin.getServer().getPlayer(shop.getOwner().getUniqueId()).sendMessage(ChatColor.GREEN + String.format("%s was taken from your account.", shop.sellNumber));
            }
        } else {
            chestInv.removeItemAnySlot(new ItemStack(Material.getMaterial(shop.sellItem), shop.sellNumber));
        }

        if (shop.buyItem.equals("")) {
            plugin.getEconomy().withdrawPlayer(player, shop.buyNumber);
            plugin.getEconomy().depositPlayer(shop.getOwner(), shop.buyNumber);
            player.sendMessage(ChatColor.GREEN + String.format("%s was taken from your account.", shop.buyNumber));
            if (shop.getOwner().isOnline()) {
                plugin.getServer().getPlayer(shop.getOwner().getUniqueId()).sendMessage(ChatColor.GREEN + String.format("%s was added to your account.", shop.buyNumber));
            }

        } else {
            playerInv.removeItemAnySlot(new ItemStack(Material.getMaterial(shop.sellItem), shop.sellNumber));
        }

    }
}
