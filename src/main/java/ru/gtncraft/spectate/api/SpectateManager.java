package ru.gtncraft.spectate.api;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import ru.gtncraft.spectate.PlayerState;
import ru.gtncraft.spectate.Spectate;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.*;

public class SpectateManager {

	int spectateTask = -1;
    final Spectate plugin;
    final Collection<Player> isSpectating = new ArrayList<>();
	final Collection<Player> isBeingSpectated = new ArrayList<>();
	final Collection<String> isClick = new ArrayList<>();
    final Collection<String> isScanning = new ArrayList<>();
    final Collection<String> inventoryOff = new ArrayList<>();
    final Map<Player, List<Player>> spectators = new HashMap<>();
    final Map<Player, Player> target = new HashMap<>();
    final Map<String, SpectateMode> playerMode = new HashMap<>();
	final Map<String, SpectateAngle> playerAngle = new HashMap<>();
    final Map<String, Integer> scanTask = new HashMap<>();
    final Map<Player, PlayerState> states = new HashMap<>();

	public SpectateManager(final Spectate plugin) {
		this.plugin = plugin;
	}

	public void startSpectateTask() {
		if (spectateTask == -1) {
			updateSpectators();
		}
	}

	public void stopSpectateTask() {
		if (spectateTask != -1) {
			plugin.getServer().getScheduler().cancelTask(spectateTask);
			spectateTask = -1;
		}
	}

	public void startSpectating(Player player, Player target, boolean saveState) {

		if (!isSpectating(player)) {
			if (saveState) {
				savePlayerState(player);
			}
		}

		for (Player player1 : plugin.getServer().getOnlinePlayers()) {
			player1.hidePlayer(player);
		}

		String playerListName = player.getPlayerListName();

		if (getSpectateAngle(player) == SpectateAngle.FIRST_PERSON) {
			player.hidePlayer(target);
		} else {
			player.showPlayer(target);
		}

		player.setPlayerListName(playerListName);

        if (target.getHealth() > 20) {
            player.setHealth(20);
        } else {
            player.setHealth(target.getHealth());
        }

		player.teleport(target);

		if (isSpectating(player)) {
			setBeingSpectated(getTarget(player), false);
			player.showPlayer(getTarget(player));
			removeSpectator(getTarget(player), player);
		}

		for (PotionEffect e : player.getActivePotionEffects()) {
			player.removePotionEffect(e.getType());
		}

		setTarget(player, target);
		addSpectator(target, player);

		player.setGameMode(target.getGameMode());
		player.setFoodLevel(target.getFoodLevel());

		setExperienceCooldown(player, Integer.MAX_VALUE);
		player.setAllowFlight(true);

		setSpectating(player, true);
		setBeingSpectated(target, true);

		player.sendMessage(ChatColor.GRAY + "You are now spectating " + target.getName() + ".");

	}

	public void stopSpectating(Player player, boolean loadState) {

		setSpectating(player, false);
		setBeingSpectated(getTarget(player), false);

		removeSpectator(getTarget(player), player);

		if (isScanning(player)) {
			stopScanning(player);
		}

		for (PotionEffect e : player.getActivePotionEffects()) {
			player.removePotionEffect(e.getType());
		}

		if (loadState) {
			loadPlayerState(player);
		}

		setExperienceCooldown(player, 0);
		player.showPlayer(getTarget(player));
	}

	public boolean scrollRight(Player player, List<Player> playerList) {

		SpectateScrollEvent event = new SpectateScrollEvent(player, playerList, ScrollDirection.RIGHT);
		plugin.getServer().getPluginManager().callEvent(event);

		playerList = new ArrayList<>(event.getSpectateList());

		playerList.remove(player);

		if (playerList.isEmpty()) {
			return false;
		}

		int scrollToIndex;

		if (getScrollNumber(player, playerList) == playerList.size()) {
			scrollToIndex = 1;
		} else {
			scrollToIndex = getScrollNumber(player, playerList) + 1;
		}

		startSpectating(player, playerList.get(scrollToIndex - 1), false);

		return true;
	}

    public boolean scrollLeft(Player p, List<Player> playerList) {

		SpectateScrollEvent event = new SpectateScrollEvent(p, playerList, ScrollDirection.LEFT);
		plugin.getServer().getPluginManager().callEvent(event);

		playerList = new ArrayList<>(event.getSpectateList());

		playerList.remove(p);

		if (playerList.isEmpty()) {
			return false;
		}

		int scrollToIndex;

		if (getScrollNumber(p, playerList) == 1) {
			scrollToIndex = playerList.size();
		} else {
			scrollToIndex = getScrollNumber(p, playerList) - 1;
		}

		startSpectating(p, playerList.get(scrollToIndex - 1), false);

		return true;
	}

	public int getScrollNumber(Player p, List<Player> playerList) {

		if (!isSpectating(p)) {
			return 1;
		}

		if (!playerList.contains(getTarget(p))) {
			return 1;
		}

		playerList.remove(p);

		return playerList.indexOf(getTarget(p)) + 1;
	}

	public void setSpectateMode(Player p, SpectateMode newMode) {
		if (newMode == SpectateMode.DEFAULT) {
			playerMode.remove(p.getName());
		} else {
			playerMode.put(p.getName(), newMode);
		}
	}

	public SpectateMode getSpectateMode(Player player) {
		if (playerMode.get(player.getName()) == null) {
			return SpectateMode.DEFAULT;
		}
		return playerMode.get(player.getName());
	}

	public void setSpectateAngle(Player p, SpectateAngle newAngle) {
		if (isSpectating(p)) {
			if (newAngle == SpectateAngle.FIRST_PERSON) {
				p.hidePlayer(getTarget(p));
			} else {
				p.showPlayer(getTarget(p));
			}
			if (newAngle == SpectateAngle.FREEROAM) {
				p.teleport(getTarget(p));
			}
		}
		if (newAngle == SpectateAngle.FIRST_PERSON) {
			playerAngle.remove(p.getName());
		} else {
			playerAngle.put(p.getName(), newAngle);
		}
	}

	public SpectateAngle getSpectateAngle(Player p) {
		if (playerAngle.get(p.getName()) == null) {
			return SpectateAngle.FIRST_PERSON;
		}
		return playerAngle.get(p.getName());

	}

	public void stopScanning(Player p) {
		Bukkit.getServer().getScheduler().cancelTask(scanTask.get(p.getName()));
		isScanning.remove(p.getName());
	}

	public boolean isScanning(Player p) {
        return isScanning.contains(p.getName());
    }

	public List<Player> getSpectateablePlayers() {
		List<Player> spectateablePlayers = new ArrayList<>();
		for (Player onlinePlayers : plugin.getServer().getOnlinePlayers()) {
			if (onlinePlayers.isDead()) {
				continue;
			}
			if (isSpectating.contains(onlinePlayers)) {
				continue;
			}
            if (onlinePlayers.hasPermission("spectate.cantspectate")) {
                continue;
            }
            spectateablePlayers.add(onlinePlayers);
		}
		return spectateablePlayers;
	}

    public Player getTarget(final Player p) {
		return target.get(p);
	}

	public boolean isSpectating(final Player player) {
		return isSpectating.contains(player);
	}

	public boolean isBeingSpectated(final Player p) {
		return isBeingSpectated.contains(p);
	}

	public List<Player> getSpectators(final Player player) {
        List<Player> result = spectators.get(player);
        if (result == null) {
            return ImmutableList.of();
        }
        return result;
	}

	public List<Player> getSpectatingPlayers() {
		List<Player> result = new ArrayList<>();
		for (Player p : plugin.getServer().getOnlinePlayers()) {
			if (isSpectating(p)) {
				result.add(p);
			}
		}
		return result;
	}

	public void disableScroll(final Player player, long ticks) {
		if (!isClick.contains(player.getName())) {
			isClick.add(player.getName());
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> isClick.remove(player.getName()), ticks);
		}
	}

	public Location getSpectateLocation(Player player) {
		
		if (getSpectateAngle(player) == SpectateAngle.FIRST_PERSON) {
			return (getTarget(player).getLocation());
		}

		Location playerLoc = getTarget(player).getLocation();

		double currentSubtraction = 0;
		Location previousLoc = playerLoc;

		while (currentSubtraction <= 5)  {

			playerLoc = getTarget(player).getLocation();

			Vector v = getTarget(player).getLocation().getDirection().normalize();
			v.multiply(currentSubtraction);

			if (getSpectateAngle(player) == SpectateAngle.THIRD_PERSON) {
				playerLoc.subtract(v);
			} else if (getSpectateAngle(player) == SpectateAngle.THIRD_PERSON_FRONT) {
				playerLoc.add(v);
				if (playerLoc.getYaw() < -180) {
					playerLoc.setYaw(playerLoc.getYaw() + 180);
				} else {
					playerLoc.setYaw(playerLoc.getYaw() - 180);
				}
				playerLoc.setPitch(-playerLoc.getPitch());
			}

			Material tempMat = new Location(playerLoc.getWorld(), playerLoc.getX(), playerLoc.getY() + 1.5, playerLoc.getZ()).getBlock().getType();

			if (tempMat != Material.AIR && tempMat != Material.WATER && tempMat != Material.STATIONARY_WATER) {
				return previousLoc;
			}

			previousLoc = playerLoc;
			currentSubtraction += 0.5;
		}
		return playerLoc;
	}


    public void setExperienceCooldown(Player p, int cooldown) {
        try {
            Method handle = p.getClass().getDeclaredMethod("getHandle");
            Object entityPlayer = handle.invoke(p);
            Field cooldownField = entityPlayer.getClass().getSuperclass().getDeclaredField("bu");
            cooldownField.setAccessible(true);
            cooldownField.setInt(entityPlayer, cooldown);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isReadyForNextScroll(Player p) {
        return ! isClick.contains(p.getName());
    }

    public double roundTwoDecimals(double d) {
        try {
            DecimalFormat twoDForm = new DecimalFormat("#.##");
            return Double.valueOf(twoDForm.format(d));
        } catch (NumberFormatException e) {
            return d;
        }
    }

	public PlayerState getPlayerState(Player p) {
		return states.get(p);
	}

	public void savePlayerState(Player p) {
		PlayerState playerstate = new PlayerState(p);
		states.put(p, playerstate);
	}

	public void loadPlayerState(Player toPlayer) {
		loadPlayerState(toPlayer, toPlayer);
	}

	public void loadPlayerState(Player fromState, Player toPlayer) {
		loadFinalState(getPlayerState(fromState), toPlayer);
		states.remove(fromState);
	}

    void updateSpectators() {

        spectateTask = Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                if (isSpectating(player)) {
                    if (getSpectateAngle(player) == SpectateAngle.FIRST_PERSON) {
                        if (roundTwoDecimals(player.getLocation().getX()) != roundTwoDecimals(getTarget(player).getLocation().getX()) || roundTwoDecimals(player.getLocation().getY()) != roundTwoDecimals(getTarget(player).getLocation().getY()) || roundTwoDecimals(player.getLocation().getZ()) != roundTwoDecimals(getTarget(player).getLocation().getZ()) || roundTwoDecimals(player.getLocation().getYaw()) != roundTwoDecimals(getTarget(player).getLocation().getYaw()) || roundTwoDecimals(player.getLocation().getPitch()) != roundTwoDecimals(getTarget(player).getLocation().getPitch())) {
                            player.teleport(getTarget(player));
                        }
                    } else {
                        if (getSpectateAngle(player) != SpectateAngle.FREEROAM) {
                            player.teleport(getSpectateLocation(player));
                        }
                    }

                    if (!inventoryOff.contains(player.getName())) {
                        player.getInventory().setContents(getTarget(player).getInventory().getContents());
                        player.getInventory().setArmorContents(getTarget(player).getInventory().getArmorContents());
                    }

                    if (getTarget(player).getHealth() == 0) {
                        player.setHealth(1);
                    } else if (getTarget(player).getHealth() > 20) {
                        player.setHealth(20);
                    } else {
                        if (getTarget(player).getHealth() < player.getHealth()) {
                            double difference = player.getHealth() - getTarget(player).getHealth();
                            player.damage(difference);
                        } else if (getTarget(player).getHealth() > player.getHealth()) {
                            player.setHealth(getTarget(player).getHealth());
                        }
                    }

                    player.setLevel(getTarget(player).getLevel());
                    player.setExp(getTarget(player).getExp());

                    for (PotionEffect e : player.getActivePotionEffects()) {

                        boolean foundPotion = false;

                        for (PotionEffect e1 : getTarget(player).getActivePotionEffects()) {
                            if (e1.getType() == e.getType()) {
                                foundPotion = true;
                                break;
                            }
                        }

                        if (!foundPotion) {
                            player.removePotionEffect(e.getType());
                        }

                    }

                    getTarget(player).getActivePotionEffects().forEach(player::addPotionEffect);

                    if (!inventoryOff.contains(player.getName())) {
                        player.getInventory().setHeldItemSlot(getTarget(player).getInventory().getHeldItemSlot());
                    }

                    if (getTarget(player).isFlying()) {
                        if (!player.isFlying()) {
                            player.setFlying(true);
                        }
                    }
                }
            }

        }, 0L, 1L);

    }

    void addSpectator(Player p, Player spectator) {
        if (spectators.get(p) == null) {
            List<Player> newSpectators = new ArrayList<>();
            newSpectators.add(spectator);
            spectators.put(p, newSpectators);
        } else {
            spectators.get(p).add(spectator);
        }
    }

    void removeSpectator(Player p, Player spectator) {
        if (spectators.get(p) != null) {
            if (spectators.get(p).size() == 1) {
                spectators.remove(p);
            } else {
                spectators.get(p).remove(spectator);
            }
        }
    }

    void setSpectating(Player player, boolean spectating) {
        if (spectating) {
            if (isSpectating.contains(player)) {
                return;
            }
            isSpectating.add(player);
        } else {
            isSpectating.remove(player);
        }
    }

    void setTarget(Player p, Player ptarget) {
        target.put(p, ptarget);
    }

    void setBeingSpectated(Player p, boolean beingSpectated) {
        if (beingSpectated) {
            if (isBeingSpectated.contains(p)) {
                return;
            }
            isBeingSpectated.add(p);
        } else {
            isBeingSpectated.remove(p);
        }
    }

    void loadFinalState(PlayerState state, Player toPlayer) {

		toPlayer.teleport(state.location);

		toPlayer.getInventory().setContents(state.inventory);
		toPlayer.getInventory().setArmorContents(state.armor);
		toPlayer.setFoodLevel(state.hunger);
		toPlayer.setHealth(state.health);
		toPlayer.setLevel(state.level);
		toPlayer.setExp(state.exp);
		toPlayer.getInventory().setHeldItemSlot(state.slot);
		toPlayer.setAllowFlight(state.allowFlight);
		toPlayer.setFlying(state.isFlying);
		toPlayer.setGameMode(state.mode);

		for (Player onlinePlayers : plugin.getServer().getOnlinePlayers()) {
			if (!state.vanishedFrom.contains(onlinePlayers)) {
				onlinePlayers.showPlayer(toPlayer);
			}
		}

        state.potions.forEach(toPlayer::addPotionEffect);
	}
}
