package de.minestar.SinCity.Listener;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import de.minestar.SinCity.Core;
import de.minestar.SinCity.Manager.AreaManager;
import de.minestar.SinCity.Manager.DataManager;
import de.minestar.SinCity.Manager.PlayerManager;
import de.minestar.SinCity.Units.SinCityPlayer;
import de.minestar.core.MinestarCore;
import de.minestar.core.units.MinestarGroup;
import de.minestar.minestarlibrary.utils.ChatUtils;

public class GriefListener implements Listener {

    private final PlayerManager playerManager;
    private final DataManager dataManager;
    private final AreaManager areaManager;

    private static HashSet<Material> disallowedStacks = new HashSet<Material>();

    static {
        // plates, leggings, helmets, boots
        disallowedStacks.add(Material.IRON_BOOTS);
        disallowedStacks.add(Material.IRON_LEGGINGS);
        disallowedStacks.add(Material.IRON_CHESTPLATE);
        disallowedStacks.add(Material.IRON_HELMET);
        disallowedStacks.add(Material.GOLD_BOOTS);
        disallowedStacks.add(Material.GOLD_LEGGINGS);
        disallowedStacks.add(Material.GOLD_CHESTPLATE);
        disallowedStacks.add(Material.GOLD_HELMET);
        disallowedStacks.add(Material.DIAMOND_BOOTS);
        disallowedStacks.add(Material.DIAMOND_LEGGINGS);
        disallowedStacks.add(Material.DIAMOND_CHESTPLATE);
        disallowedStacks.add(Material.DIAMOND_HELMET);
        disallowedStacks.add(Material.LEATHER_BOOTS);
        disallowedStacks.add(Material.LEATHER_LEGGINGS);
        disallowedStacks.add(Material.LEATHER_CHESTPLATE);
        disallowedStacks.add(Material.LEATHER_HELMET);
        // horse
        disallowedStacks.add(Material.SADDLE);
        disallowedStacks.add(Material.IRON_BARDING);
        disallowedStacks.add(Material.GOLD_BARDING);
        disallowedStacks.add(Material.DIAMOND_BARDING);
        // weapons
        disallowedStacks.add(Material.BOW);
        // sword
        disallowedStacks.add(Material.DIAMOND_SWORD);
        disallowedStacks.add(Material.GOLD_SWORD);
        disallowedStacks.add(Material.IRON_SWORD);
        disallowedStacks.add(Material.WOOD_SWORD);
        // axe
        disallowedStacks.add(Material.DIAMOND_AXE);
        disallowedStacks.add(Material.GOLD_AXE);
        disallowedStacks.add(Material.IRON_AXE);
        disallowedStacks.add(Material.WOOD_AXE);
        // spade
        disallowedStacks.add(Material.DIAMOND_SPADE);
        disallowedStacks.add(Material.GOLD_SPADE);
        disallowedStacks.add(Material.IRON_SPADE);
        disallowedStacks.add(Material.WOOD_SPADE);
        // pickaxe
        disallowedStacks.add(Material.DIAMOND_PICKAXE);
        disallowedStacks.add(Material.GOLD_PICKAXE);
        disallowedStacks.add(Material.IRON_PICKAXE);
        disallowedStacks.add(Material.WOOD_PICKAXE);
        // hoe
        disallowedStacks.add(Material.DIAMOND_HOE);
        disallowedStacks.add(Material.GOLD_HOE);
        disallowedStacks.add(Material.IRON_HOE);
        disallowedStacks.add(Material.WOOD_HOE);
    }

    public GriefListener(DataManager dataManager, PlayerManager playerManager, AreaManager areaManager) {
        this.dataManager = dataManager;
        this.playerManager = playerManager;
        this.areaManager = areaManager;
    }

    private boolean denyBlockAction(Player player, Block block) {
        SinCityPlayer thisPlayer = this.playerManager.getPlayer(player);

        boolean insideArea = this.areaManager.isInside(block.getWorld().getName(), block.getX(), block.getZ());
        insideArea = insideArea && MinestarCore.getPlayer(player).getMinestarGroup().getLevel() > MinestarGroup.DEFAULT.getLevel();

        // CHECK FOR DENIAL
        if (!insideArea && this.dataManager.isInDenyAll(thisPlayer.getGroup(), block.getWorld().getName())) {
            ChatUtils.writeError(player, Core.NAME, "Du kannst hier nicht abbauen.");
            return true;
        }

        // CHECK FOR PARTIAL DENIAL
        if (!insideArea && this.dataManager.isInDenyPartial(thisPlayer.getGroup(), block.getWorld().getName())) {
            ChatUtils.writeError(player, Core.NAME, "Du kannst hier nicht abbauen.");
            return true;
        }

        return false;
    }

    @EventHandler
    public void onItemPickup(PlayerPickupItemEvent event) {
        Player player = event.getPlayer();
        InventoryView inventoryView = player.getOpenInventory();
        if (inventoryView != null) {
            if (inventoryView.getType().equals(InventoryType.MERCHANT)) {
                event.setCancelled(true);
                return;
            }
            Inventory inventory = inventoryView.getTopInventory();
            if (inventory != null) {
                if (inventory.getType().equals(InventoryType.MERCHANT)) {
                    event.setCancelled(true);
                    return;
                }
            }
            inventory = inventoryView.getBottomInventory();
            if (inventory != null) {
                if (inventory.getType().equals(InventoryType.MERCHANT)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        // Only handle ItemFrames & Paintings
        if (!event.getEntity().getType().equals(EntityType.ITEM_FRAME) && !event.getEntity().getType().equals(EntityType.PAINTING)) {
            return;
        }
        if (event.getRemover().getType().equals(EntityType.PLAYER)) {
            // get the player
            Player player = (Player) event.getRemover();
            SinCityPlayer thisPlayer = this.playerManager.getPlayer(player);

            // handle
            if (this.dataManager.isInDenyAll(thisPlayer.getGroup(), player.getWorld().getName())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingInteract(PlayerInteractEntityEvent event) {
        // Only handle ItemFrames & Paintings
        if (!event.getRightClicked().getType().equals(EntityType.ITEM_FRAME) && !event.getRightClicked().getType().equals(EntityType.PAINTING)) {
            return;
        }

        // handle
        Player player = (Player) event.getPlayer();
        SinCityPlayer thisPlayer = this.playerManager.getPlayer(player);

        if (this.dataManager.isInDenyAll(thisPlayer.getGroup(), player.getWorld().getName())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingDamage(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Entity damager = event.getDamager();

        // Only handle ItemFrames & Paintings
        if (!entity.getType().equals(EntityType.ITEM_FRAME) && !entity.getType().equals(EntityType.PAINTING)) {
            return;
        }

        if (damager.getType().equals(EntityType.PLAYER)) {
            Player player = (Player) damager;
            SinCityPlayer thisPlayer = this.playerManager.getPlayer(player);

            if (this.dataManager.isInDenyAll(thisPlayer.getGroup(), player.getWorld().getName())) {
                event.setCancelled(true);
            }
        }
    }

    private boolean denyPlayerDamage(Player player) {
        SinCityPlayer thisPlayer = this.playerManager.getPlayer(player);

        // CHECK FOR DENIAL
        if (this.dataManager.isInDenyAll(thisPlayer.getGroup(), player.getWorld().getName())) {
            return true;
        }

        return false;
    }

    private boolean denySpecialInteractAction(Player player, Block block) {
        SinCityPlayer thisPlayer = this.playerManager.getPlayer(player);

        boolean insideArea = this.areaManager.isInside(block.getWorld().getName(), block.getX(), block.getZ());
        insideArea = insideArea && MinestarCore.getPlayer(player).getMinestarGroup().getLevel() > MinestarGroup.DEFAULT.getLevel();

        // CHECK FOR DENIAL
        if (!insideArea && this.dataManager.isInDenyAll(thisPlayer.getGroup(), player.getWorld().getName())) {
            // CHECK BLOCK
            Material mat = block.getType();
            switch (mat) {
                case DIODE_BLOCK_ON :
                case DIODE_BLOCK_OFF :
                case CHEST :
                case DISPENSER :
                case FURNACE :
                case BURNING_FURNACE :
                    ChatUtils.writeError(player, Core.NAME, "Du kannst hier nicht ver�ndern.");
                    return true;
                default :
                    // do nothing
            }
        }

        return false;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event) {
        // EVENT IS CANCELLED? => RETURN
        if (event.isCancelled())
            return;

        // UPDATE EVENT-STATE
        event.setCancelled(this.denyBlockAction(event.getPlayer(), event.getBlock()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event) {
        // EVENT IS CANCELLED? => RETURN
        if (event.isCancelled())
            return;

        // UPDATE EVENT-STATE
        event.setCancelled(this.denyBlockAction(event.getPlayer(), event.getBlock()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketEmpty(PlayerBucketEmptyEvent event) {
        // EVENT IS CANCELLED? => RETURN
        if (event.isCancelled())
            return;

        // UPDATE EVENT-STATE
        event.setCancelled(this.denyBlockAction(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace())));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBucketFill(PlayerBucketFillEvent event) {
        // EVENT IS CANCELLED? => RETURN
        if (event.isCancelled())
            return;

        // UPDATE EVENT-STATE
        event.setCancelled(this.denyBlockAction(event.getPlayer(), event.getBlockClicked().getRelative(event.getBlockFace())));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        // EVENT IS CANCELLED? => RETURN
        if (event.isCancelled())
            return;

        // HAS BLOCK?
        if (!event.hasBlock())
            return;

        // UPDATE EVENT-STATE
        event.setCancelled(this.denySpecialInteractAction(event.getPlayer(), event.getClickedBlock()));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
        // EVENT IS CANCELLED? => RETURN
        if (event.isCancelled())
            return;

        // UPDATE EVENT-STATE
        if (event.getDamager().getType().equals(EntityType.PLAYER)) {
            Player attacker = (Player) event.getDamager();
            // ATTACKING PLAYER?
            if (event.getEntity().getType().equals(EntityType.PLAYER)) {
                Player defender = (Player) event.getEntity();
                // SEND MESSAGE, IF EVENT IS CANCELLED AND DEAL DAMAGE
                if (this.denyPlayerDamage(attacker)) {
                    attacker.damage(7.0);
                    event.setCancelled(true);
                    ChatUtils.writeError(attacker, "Nicht die Mama!");
                    this.playerManager.sendToOps("'" + attacker.getName() + "' betreibt PVP an '" + defender.getName() + "'.");
                }
            } else {
                if (event.getEntity().getType().equals(EntityType.VILLAGER)) {
                    ChatUtils.writeError(attacker, "I AM LEGEND!");
                    event.setCancelled(true);
                    return;
                } else if (event.getEntity().getType().equals(EntityType.COW) || event.getEntity().getType().equals(EntityType.CHICKEN) || event.getEntity().getType().equals(EntityType.SHEEP) || event.getEntity().getType().equals(EntityType.IRON_GOLEM) || event.getEntity().getType().equals(EntityType.MUSHROOM_COW) || event.getEntity().getType().equals(EntityType.OCELOT) || event.getEntity().getType().equals(EntityType.SNOWMAN) || event.getEntity().getType().equals(EntityType.PIG) || event.getEntity().getType().equals(EntityType.VILLAGER) || event.getEntity().getType().equals(EntityType.WOLF)) {
                    if (this.denyPlayerDamage(attacker)) {
                        attacker.damage(7.0);
                        attacker.setVelocity(new Vector(0, 5, 0));
                        event.setCancelled(true);
                        ChatUtils.writeError(attacker, "Eine wilde Faaaaaahhhhrrrrrttt.....");
                        this.playerManager.sendToOps("'" + attacker.getName() + "' betreibt PVP an '" + event.getEntity().getType().name() + "'.");
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void preventToolStacks(InventoryClickEvent event) {
        InventoryType destType = event.getInventory().getType();
        if (destType == InventoryType.ANVIL || destType == InventoryType.ENCHANTING) {
            if (event.getSlotType() == SlotType.QUICKBAR || event.getSlotType() == SlotType.CONTAINER) {
                ItemStack item = event.getCurrentItem();
                if (item == null) {
                    return;
                }

                if (item.getAmount() > 1 && disallowedStacks.contains(item.getType())) {
                    event.setCancelled(true);
                }
            }
        }
    }
}
