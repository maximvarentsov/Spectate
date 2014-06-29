package ru.gtncraft.spectate;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.gtncraft.spectate.api.SpectateManager;

public final class Spectate extends JavaPlugin {

	SpectateManager manager;

    @Override
	public void onEnable() {

        manager = new SpectateManager(this);

        new Listeners(this);
		new Commands(this);

        getManager().startSpectateTask();
	}

    @Override
	public void onDisable() {
		for (Player player : getManager().getSpectatingPlayers()) {
			getManager().stopSpectating(player, true);
			player.sendMessage(ChatColor.GRAY + "You were forced to stop spectating because plugins disabled.");
			
		}
		getManager().stopSpectateTask();
	}
	
	public SpectateManager getManager() {
		return manager;
	}
}
