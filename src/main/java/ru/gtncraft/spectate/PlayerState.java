package ru.gtncraft.spectate;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;

public final class PlayerState {

	public Player player;
    public ItemStack[] inventory, armor;
    public int hunger, slot, level;
    public double health;
    public float exp;
    public boolean allowFlight, isFlying;
    public GameMode mode;
    public Location location;
	
	public Collection<PotionEffect> potions;
	public Collection<Player> vanishedFrom = new ArrayList<>();

	public PlayerState(Player p) {
		player = p;
		inventory = p.getInventory().getContents();
		armor = p.getInventory().getArmorContents();
		hunger = p.getFoodLevel();
		health = p.getHealth();
		level = p.getLevel();
		exp = p.getExp();
		slot = p.getInventory().getHeldItemSlot();
		allowFlight = p.getAllowFlight();
		isFlying = p.isFlying();
		mode = p.getGameMode();
		location = p.getLocation();
		
		potions = p.getActivePotionEffects();

		for (Player players : Bukkit.getServer().getOnlinePlayers()) {
			if (players != p) {
				if (!players.canSee(p)) {
					vanishedFrom.add(players);
				}
			}
		}
	}
}
