package ru.gtncraft.spectate;

import com.google.common.collect.ImmutableList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import ru.gtncraft.spectate.api.SpectateAngle;
import ru.gtncraft.spectate.api.SpectateManager;
import ru.gtncraft.spectate.api.SpectateMode;

import java.util.List;

final class Commands implements CommandExecutor, TabExecutor {
	
	final SpectateManager manager;
	
	public Commands(final Spectate plugin) {
        Bukkit.getPluginCommand("spectate").setExecutor(this);
        manager = plugin.getManager();
	}

    @Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "You can't execute this command from the console.");
            return true;
        }

        if (args.length == 0) {
            return false;
        }

        Player player = (Player) sender;
        player.sendMessage(subcommand(player, args));

        return true;
	}

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "off":
                case "mode":
                    return ImmutableList.of();
                case "angle":
                    return ImmutableList.of("firstperson", "thirdperson", "thirdpersonfront", "freeroam");
            }
        }
        return null;
    }

    String subcommand(final Player player, String[] args) {

        switch (args[0].toLowerCase()) {
            case "off":
                return off(player);
            case "mode":
                return mode(player);
            case "angle":
                return angle(player, args);
        }

        return spectate(player, args);
    }

    String off(final Player player) {
        if (!manager.isSpectating(player)) {
            return ChatColor.GRAY + "You are not currently spectating anyone.";
        }
        manager.stopSpectating(player, true);
        return ChatColor.GRAY + "You have stopped spectating " + manager.getTarget(player).getName() + ".";
    }

    String mode(final Player player) {

        if (!player.hasPermission("spectate.mode")) {
            return ChatColor.RED + "You do not have permission.";
        }

        if (manager.getSpectateMode(player) == SpectateMode.DEFAULT) {
            manager.setSpectateMode(player, SpectateMode.SCROLL);
            return ChatColor.GRAY + "You are now using the scroll spectate mode.";
        } else {
            manager.setSpectateMode(player, SpectateMode.DEFAULT);
            return ChatColor.GRAY + "You are now using the default spectate mode.";
        }
    }

    String angle(Player player, String[] args) {

        if (!player.hasPermission("spectate.angle")) {
            return ChatColor.RED + "You do not have permission.";
        }

        if (args.length < 2) {
            return ChatColor.RED + "Error: You must enter in an angle.";
        }

        switch (args[1].toLowerCase()) {

            case "firstperson":
                if (manager.getSpectateAngle(player) == SpectateAngle.FIRST_PERSON) {
                    return ChatColor.RED + "Error: You are already in first person mode.";
                }
                manager.setSpectateAngle(player, SpectateAngle.FIRST_PERSON);
                return ChatColor.GRAY + "You are now in first person mode.";

            case "thirdperson":
                if (manager.getSpectateAngle(player) == SpectateAngle.THIRD_PERSON) {
                    return ChatColor.RED + "Error: You are already in third person mode.";
                }
                manager.setSpectateAngle(player, SpectateAngle.THIRD_PERSON);
                return ChatColor.GRAY + "You are now in third person mode.";

            case "thirdpersonfront":
                if (manager.getSpectateAngle(player) == SpectateAngle.THIRD_PERSON_FRONT) {
                    return ChatColor.RED + "Error: You are already in third person front mode.";
                }
                manager.setSpectateAngle(player, SpectateAngle.THIRD_PERSON_FRONT);
                return ChatColor.GRAY + "You are now in third person front mode.";

            case "freeroam":
                if (manager.getSpectateAngle(player) == SpectateAngle.FREEROAM) {
                    return ChatColor.RED + "Error: You are already in free roam mode.";
                }
                manager.setSpectateAngle(player, SpectateAngle.FREEROAM);
                return ChatColor.GRAY + "You are now in free roam mode.";
        }

        return ChatColor.RED + "Unknown angle " + args[1];
    }

    String spectate(final Player player, String[] args) {

        if (!player.hasPermission("spectate.on")) {
            return ChatColor.RED + "You do not have permission.";
        }

        Player target = Bukkit.getServer().getPlayer(args[0]);

        if (target == null) {
            return ChatColor.RED + "Error: Player is not online.";
        }

        if (player.getName().equals(target.getName())) {
            return ChatColor.GRAY + "Did you really just try to spectate yourself?";
        }

        if (target.hasPermission("spectate.cantspectate")) {
            return ChatColor.GRAY + "This person can not be spectated.";
        }

        if (manager.isSpectating(player)) {
            if (target.getName().equals(manager.getTarget(player).getName())) {
                return ChatColor.GRAY + "You are already spectating them.";
            }
        }

        if (manager.isSpectating(target)) {
            return ChatColor.GRAY + "They are currently spectating someone.";
        }

        if (target.isDead()) {
            return ChatColor.GRAY + "They are currently dead.";
        }

        manager.startSpectating(player, target, true);
        return ChatColor.GRAY + "You are spectate player " + target.getName();
    }
}
