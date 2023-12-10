package questy.questy;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

public final class Questy extends JavaPlugin implements Listener, CommandExecutor {

    private FileConfiguration config;

    @Override
    public void onEnable() {
        getCommand("quest").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
        config = getConfig();
        saveDefaultConfig();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("player_only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 1) {
            String questName = args[0];

            if (questName.equalsIgnoreCase("walkquest") || questName.equalsIgnoreCase("killzombiequest")) {
                if (config.getString(player.getUniqueId() + ".quest") != null) {
                    player.sendMessage(getMessage("active_quest"));
                    return true;
                }

                config.set(player.getUniqueId() + ".progress", 0);

                config.set(player.getUniqueId() + ".quest", questName);
                saveConfig();

                player.sendMessage(getMessage("start_quest_walking").replace("%quest%", questName));
            } else if (questName.equalsIgnoreCase("killzombiequest")) {
                if (config.getString(player.getUniqueId() + ".quest") != null) {
                    player.sendMessage(getMessage("active_quest"));
                    return true;
                }

                config.set(player.getUniqueId() + ".progress", 0);

                config.set(player.getUniqueId() + ".quest", questName);
                saveConfig();

                player.sendMessage(getMessage("start_quest_zombie").replace("%quest%", questName));

            } else if (questName.equalsIgnoreCase("reset")) {
                resetQuest(player);
                player.sendMessage(getMessage("reset_quest"));
            } else {
                player.sendMessage(getMessage("unknown_quest"));
            }

        } else if (args.length == 0){
            openGUI(player);
        } else {
            player.sendMessage(getMessage("usage"));
        }

        return true;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String quest = config.getString(player.getUniqueId() + ".quest");

        if (quest != null && quest.equalsIgnoreCase("walkquest")) {
            int progress = config.getInt(player.getUniqueId() + ".progress");
            Location from = event.getFrom();
            Location to = event.getTo();

            if (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ()) {
                config.set(player.getUniqueId() + ".progress", progress + 1);
                saveConfig();

                if (progress + 1 >= 200) {
                    player.sendMessage(getMessage("complete_quest_200blocks"));
                    resetQuest(player);
                }
            }
        } else if (quest != null && quest.equalsIgnoreCase("killzombiequest")) {
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getType() == EntityType.ZOMBIE) {
            if (event.getEntity().getKiller() instanceof Player) {
                Player player = (Player) event.getEntity().getKiller();
                String quest = config.getString(player.getUniqueId() + ".quest");

                if (quest != null && quest.equalsIgnoreCase("killzombiequest")) {
                    int zombieCount = config.getInt(player.getUniqueId() + ".zombieCount", 0);
                    config.set(player.getUniqueId() + ".zombieCount", zombieCount + 1);
                    saveConfig();

                    if (zombieCount + 1 >= 20) {
                        player.sendMessage(getMessage("complete_quest_kill_zombie"));
                        resetQuest(player);
                    }
                }
            }
        }
    }

    private void resetQuest(Player player) {
        config.set(player.getUniqueId() + ".quest", null);
        config.set(player.getUniqueId() + ".progress", null);
        config.set(player.getUniqueId() + ".zombieCount", null);
        saveConfig();
    }
    private String getMessage(String path) {
        String message = config.getString("messages." + path, null);

        if (message != null) {
            return ChatColor.translateAlternateColorCodes('&', message);
        } else {
            return "";
        }
    }
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle().equals("Quest Selection")) {
            event.setCancelled(true);

            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.LEATHER_BOOTS) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                Bukkit.dispatchCommand(player, "quest walkquest");
            }
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.IRON_SWORD) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                Bukkit.dispatchCommand(player, "quest killzombiequest");
            }
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.BARRIER) {
                Player player = (Player) event.getWhoClicked();
                player.closeInventory();
                Bukkit.dispatchCommand(player, "quest reset");
            }
        }
    }

    private void openGUI(Player player) {
        Inventory gui = Bukkit.createInventory(player, 9, "Quest Selection");

        ItemStack item1 = new ItemStack(Material.LEATHER_BOOTS);
        ItemMeta m = item1.getItemMeta();
        m.setDisplayName(ChatColor.YELLOW + "Walk 200 blocks");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "You have to walk 200 blocks!");
        m.setLore(lore);
        item1.setItemMeta(m);

        ItemStack item2 = new ItemStack(Material.IRON_SWORD);
        ItemMeta m2 = item2.getItemMeta();
        m2.setDisplayName(ChatColor.YELLOW + "Zombie Slayer");
        List<String> lore2 = new ArrayList<>();
        lore2.add(ChatColor.GRAY + "You have to kill 20 zombies!");
        m2.setLore(lore2);
        item2.setItemMeta(m2);

        ItemStack item3 = new ItemStack(Material.BARRIER);
        ItemMeta m3 = item3.getItemMeta();
        m3.setDisplayName(ChatColor.RED + "Reset Quest");
        List<String> lore3 = new ArrayList<>();
        lore3.add(ChatColor.GRAY + "Click to reset your quest!");
        m3.setLore(lore3);
        item3.setItemMeta(m3);



        gui.setItem(3, item1);
        gui.setItem(4, item3);
        gui.setItem(5, item2);


        player.openInventory(gui);
    }
}