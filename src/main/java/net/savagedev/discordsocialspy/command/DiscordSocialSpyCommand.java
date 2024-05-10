package net.savagedev.discordsocialspy.command;

import net.savagedev.discordsocialspy.DiscordSocialSpyPlugin;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.Locale;

public class DiscordSocialSpyCommand implements CommandExecutor {
    private final DiscordSocialSpyPlugin plugin;

    public DiscordSocialSpyCommand(DiscordSocialSpyPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            return false;
        }

        final String action = args[0].toLowerCase(Locale.ROOT);

        if (!action.equals("reload")) {
            return false;
        }

        this.plugin.reload();
        sender.sendMessage("Plugin reloaded!");
        return true;
    }
}
