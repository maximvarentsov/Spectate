package ru.gtncraft.spectate;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import ru.gtncraft.spectate.api.ScrollDirection;
import ru.gtncraft.spectate.api.SpectateManager;
import ru.gtncraft.spectate.api.SpectateMode;
import ru.gtncraft.spectate.api.SpectateScrollEvent;

import java.util.List;

final class Listeners implements Listener {

    final SpectateManager manager;

	public Listeners(final Spectate plugin) {
        manager = plugin.getManager();
        Bukkit.getServer().getPluginManager().registerEvents(this, plugin);
	}

	@EventHandler
    @SuppressWarnings("unused")
	void onPlayerJoin(final PlayerJoinEvent event) {
		for (Player player : manager.getSpectatingPlayers()) {
			event.getPlayer().hidePlayer(player);
		}
	}

	@EventHandler
    @SuppressWarnings("unused")
	void onPlayerQuit(final PlayerQuitEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
			manager.stopSpectating(event.getPlayer(), true);
        } else if (manager.isBeingSpectated(event.getPlayer())) {
			for (Player p : manager.getSpectators(event.getPlayer())) {
				if (manager.getSpectateMode(p) == SpectateMode.SCROLL || manager.isScanning(p)) {
					SpectateScrollEvent scrollEvent = new SpectateScrollEvent(p, manager.getSpectateablePlayers(), ScrollDirection.RIGHT);
					Bukkit.getServer().getPluginManager().callEvent(scrollEvent);
					List<Player> playerList = scrollEvent.getSpectateList();
					playerList.remove(p);
					playerList.remove(event.getPlayer());
					p.sendMessage(ChatColor.GRAY + "The person you were previously spectating has disconnected.");
					if (!manager.scrollRight(p, playerList)) {
						manager.stopSpectating(p, true);
						p.sendMessage(ChatColor.GRAY + "You were forced to stop spectating because there is nobody left to spectate.");
					}
				} else {
					manager.stopSpectating(p, true);
					p.sendMessage(ChatColor.GRAY + "You were forced to stop spectating because the person you were spectating disconnected.");
				}
			}
        }
	}

	@EventHandler
    @SuppressWarnings("unused")
	void onPlayerDeath(final PlayerDeathEvent event) {
		if (manager.isBeingSpectated(event.getEntity())) {
			for (Player p : manager.getSpectators(event.getEntity())) {
				if (manager.getSpectateMode(p) == SpectateMode.SCROLL || manager.isScanning(p)) {
					SpectateScrollEvent scrollEvent = new SpectateScrollEvent(p, manager.getSpectateablePlayers(), ScrollDirection.RIGHT);
					Bukkit.getServer().getPluginManager().callEvent(scrollEvent);
					List<Player> playerList = scrollEvent.getSpectateList();
					playerList.remove(p);
					playerList.remove(event.getEntity());
					p.sendMessage(ChatColor.GRAY + "The person you were previously spectating has died.");
					if (!manager.scrollRight(p, playerList)) {
						manager.stopSpectating(p, true);
						p.sendMessage(ChatColor.GRAY + "You were forced to stop spectating because there is nobody left to spectate.");
					}
				} else {
					manager.stopSpectating(p, true);
					p.sendMessage(ChatColor.GRAY + "You were forced to stop spectating because the person you were spectating died.");
				}
			}
        }
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
	void onPlayerDamage(final EntityDamageEvent event) {
		if (event instanceof EntityDamageByEntityEvent) {
			EntityDamageByEntityEvent event1 = (EntityDamageByEntityEvent) event;
			if (event1.getDamager() instanceof Player) {
				if (manager.isSpectating((Player)event1.getDamager())) {
					event.setCancelled(true);
					return;
				}
			}
		}
		if (!(event.getEntity() instanceof Player)) {
			return;
		}
		Player p = (Player) event.getEntity();
		if (manager.isSpectating(p)) {
			event.setCancelled(true);
        }
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
	void onPlayerInteract(final PlayerInteractEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
			if (manager.isReadyForNextScroll(event.getPlayer())) {
				if (manager.getSpectateMode(event.getPlayer()) == SpectateMode.SCROLL) {
					if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
						if (Bukkit.getServer().getOnlinePlayers().size() > 2) {
							manager.scrollLeft(event.getPlayer(), manager.getSpectateablePlayers());
							manager.disableScroll(event.getPlayer(), 5);
						}
					} else if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
						if (Bukkit.getServer().getOnlinePlayers().size() > 2) {
							manager.scrollRight(event.getPlayer(), manager.getSpectateablePlayers());
							manager.disableScroll(event.getPlayer(), 5);
						}
					}
				}
			}
			event.setCancelled(true);
        }
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
	void onPlayerInteractEntity(final PlayerInteractEntityEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
			if (manager.isReadyForNextScroll(event.getPlayer())) {
				if (manager.getSpectateMode(event.getPlayer()) == SpectateMode.SCROLL) {
					if (Bukkit.getServer().getOnlinePlayers().size() > 2) {
						manager.scrollRight(event.getPlayer(), manager.getSpectateablePlayers());
						manager.disableScroll(event.getPlayer(), 5);
					}
				}
			}
			event.setCancelled(true);
        }
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
	void onFoodLevelChange(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
            if (manager.isBeingSpectated(player)) {
                for (Player p : manager.getSpectators(player)) {
                    p.setFoodLevel(event.getFoodLevel());
                }
            }
		}
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onPlayerGameModeChange(final PlayerGameModeChangeEvent event) {
        if (manager.isBeingSpectated(event.getPlayer())) {
            for (Player player : manager.getSpectators(event.getPlayer())) {
                player.setGameMode(event.getNewGameMode());
            }
        }
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
	void onInventoryOpen(final InventoryOpenEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getPlayer();
		if (manager.isBeingSpectated(player)) {
			for (Player spectators : manager.getSpectators(player)) {
				spectators.openInventory(event.getInventory());
			}
		}
	}

	@EventHandler
    @SuppressWarnings("unused")
    void onInventoryClose(final InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getPlayer();
		if (manager.isBeingSpectated(player)) {
			for (Player spectators : manager.getSpectators(player)) {
				spectators.closeInventory();
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onInventoryClick(final InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
		Player player = (Player) event.getWhoClicked();
		if (manager.isSpectating(player)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onPlayerDropItem(final PlayerDropItemEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onPlayerPickupItem(final PlayerPickupItemEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onBlockBreak(final BlockBreakEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
			event.setCancelled(true);
		}
	}

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onBlockPlace(final BlockPlaceEvent event) {
		if (manager.isSpectating(event.getPlayer())) {
            event.setCancelled(true);
        }
	}

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onPlayerRegen(final EntityRegainHealthEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			if (manager.isSpectating(player)) {
				event.setCancelled(true);
			}
		}
	}

    @EventHandler(ignoreCancelled = true)
    @SuppressWarnings("unused")
    void onMobTarget(final EntityTargetEvent event) {
		if (event.getEntity() instanceof Monster) {
			if (event.getTarget() instanceof Player) {
				if (manager.isSpectating(((Player)event.getTarget()))) {
					event.setCancelled(true);
				}
			}
		}
	}
}
