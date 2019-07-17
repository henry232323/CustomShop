package henry232323.plugin.customshop;

import com.destroystokyo.paper.event.block.BlockDestroyEvent;
import org.bukkit.Location;
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
        Block block = event.getClickedBlock();

        if (block == null) {
            return;
        }

        if (block.getBlockData() instanceof Sign || block.getBlockData() instanceof WallSign) {
            if (block.hasMetadata("shop")) {
                List<MetadataValue> mdv = block.getMetadata("shop");
                for (MetadataValue val : mdv) {
                    if (val.getOwningPlugin() == plugin) {
                        Shop shop = (Shop) val.value();
                        playerShopInteract(event.getPlayer(), shop);
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
                    String line1 = event.getLines()[1];
                    String part1 = line1.substring(0, line1.indexOf(' '));
                    String part2 = line1.substring(part1.length(), line1.length());

                    buyAmount = Integer.parseInt(part1.trim());
                    buyItem = part2.toUpperCase().trim();
                    Material mat = Material.matchMaterial(buyItem.toUpperCase().trim());
                    if (mat == null) {
                        event.getPlayer().sendMessage(ChatColor.RED + "Malformed sign, invalid item at line 2");
                        return;
                    }

                    if (buyAmount > new ItemStack(mat).getMaxStackSize()) {
                        event.getPlayer().sendMessage(ChatColor.RED + "Malformed sign, too many items at line 2");
                        return;
                    }
                }

                String sellItem = "";
                int sellAmount = 0;

                if (event.getLines()[2].charAt(0) == '$') {
                    sellAmount = Integer.parseInt(event.getLines()[2].substring(1, event.getLines()[2].length()));
                } else {
                    String line2 = event.getLines()[2];
                    String part1 = line2.substring(0, line2.indexOf(' '));
                    String part2 = line2.substring(part1.length() + 1, line2.length());

                    sellAmount = Integer.parseInt(part1.trim());
                    sellItem = part2.toUpperCase().trim();
                    Material mat = Material.matchMaterial(sellItem.toUpperCase().trim());
                    if (mat == null) {
                        event.getPlayer().sendMessage(ChatColor.RED + "Malformed sign, invalid item at line 2");
                        return;
                    }

                    if (sellAmount > new ItemStack(mat).getMaxStackSize()) {
                        event.getPlayer().sendMessage(ChatColor.RED + "Malformed sign, too many items at line 3");
                        return;
                    }
                }

                org.bukkit.block.Sign sign = (org.bukkit.block.Sign) block.getState();
                sign.setLine(3, event.getPlayer().getDisplayName());
                event.getPlayer().sendMessage(ChatColor.GREEN + "Successfully created new shop!");

                Shop shop = new Shop(event.getPlayer(), sign.getLocation(), buyAmount, sellAmount, buyItem, sellItem);
                shop.setPlugin(plugin);
                sign.setMetadata("shop", new FixedMetadataValue(plugin, shop));
                plugin.shops.add(shop);
                plugin.save(plugin.shops, new File(plugin.getDataFolder(), "shops.dat"));

            } catch (Exception e) {
                event.getPlayer().sendMessage(ChatColor.RED + "Malformed sign, exception");
                e.printStackTrace();
                return;
            }
        }

    }

    public void playerShopInteract(Player player, Shop shop) {
        Location pos = shop.getStoragePosition();
        Block block = pos.getBlock();
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
            if (plugin.getEconomy().has(shop.getOwner(), shop.sellNumber)) {
                buyerCheck = true;
            }
        } else if (playerInv.firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "You do not have any space in your inventory!");
            return;
        } else  {
            if (chestInv.contains(Material.matchMaterial(shop.sellItem), shop.sellNumber)) {
                buyerCheck = true;
            }
        }

        if (shop.buyItem.equals("")) {
            if (plugin.getEconomy().has(player, shop.buyNumber)) {
                sellerCheck = true;
            }
        } else if (chestInv.firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "This shop is out of room to buy!");
        } else if (playerInv.contains(Material.matchMaterial(shop.buyItem), shop.buyNumber)) {
            sellerCheck = true;
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

            player.sendMessage(ChatColor.GREEN + String.format("$%s was deposited into your account.", shop.sellNumber));
            if (shop.getOwner().isOnline()) {
                plugin.getServer().getPlayer(shop.getOwner().getUniqueId()).sendMessage(ChatColor.GREEN + String.format("$%s was taken from your account.", shop.sellNumber));
            }
        } else {
            ItemStack stack = new ItemStack(Material.matchMaterial(shop.sellItem), shop.sellNumber);
            chestInv.removeItemAnySlot(stack);
            playerInv.addItem(stack);

            player.sendMessage(String.format("You have received %s %s", shop.sellNumber, shop.sellItem));
        }

        if (shop.buyItem.equals("")) {
            plugin.getEconomy().withdrawPlayer(player, shop.buyNumber);
            plugin.getEconomy().depositPlayer(shop.getOwner(), shop.buyNumber);
            player.sendMessage(ChatColor.GREEN + String.format("$%s was taken from your account.", shop.buyNumber));
            if (shop.getOwner().isOnline()) {
                plugin.getServer().getPlayer(shop.getOwner().getUniqueId()).sendMessage(ChatColor.GREEN + String.format("$%s was added to your account.", shop.buyNumber));
            }

        } else {
            ItemStack stack = new ItemStack(Material.matchMaterial(shop.buyItem), shop.buyNumber);
            playerInv.removeItemAnySlot(stack);
            chestInv.addItem(stack);
        }

    }
}
