package net.savagedev.discordsocialspy;

import net.savagedev.discordsocialspy.command.DiscordSocialSpyCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordSocialSpyPlugin extends JavaPlugin implements Listener {
    private final Map<String, String> commandGroups = new HashMap<>();
    private final HttpClient client = HttpClient.newHttpClient();

    private String bodyFormat;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        this.saveResource("embed.json", false);
        this.loadBodyFormat();
        this.computeCommandGroups();
        this.getServer().getPluginManager().registerEvents(this, this);
        this.getCommand("discordsocialspy").setExecutor(new DiscordSocialSpyCommand(this));
    }

    public void reload() {
        this.reloadConfig();
        this.loadBodyFormat();
        this.commandGroups.clear();
        this.computeCommandGroups();
    }

    @EventHandler
    public void on(PlayerCommandPreprocessEvent event) {
        final String fullCommand = event.getMessage().replace("/", "");
        final String command = fullCommand.split(" ")[0];

        if (!this.commandGroups.containsKey(command)) {
            return;
        }

        this.executeWebhook(event.getPlayer(), fullCommand);
    }

    private void loadBodyFormat() {
        final StringBuilder bodyFormat = new StringBuilder();

        try (final BufferedReader reader = Files.newBufferedReader(this.getDataFolder().toPath().resolve("embed.json"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyFormat.append(line).append("\n");
            }
        } catch (IOException e) {
            this.getLogger().warning("Failed to read fody format from embeds.json.");
        }

        this.bodyFormat = bodyFormat.toString().trim();
    }

    private String applyBodyFormat(Player player, String commandGroup, String fullCommand) {
        return this.bodyFormat.replace("%sender_username%", player.getName())
                .replace("%timestamp%", Instant.now().toString())
                .replace("%sender_uuid%", player.getUniqueId().toString().replace("-", ""))
                .replace("%group_name%", commandGroup)
                .replace("%full_command%", fullCommand)
                .replace("%root_command%", fullCommand.split(" ", 1)[0]);
    }

    private void computeCommandGroups() {
        final ConfigurationSection section = this.getConfig().getConfigurationSection("groups");

        if (section == null) {
            return;
        }

        for (String group : section.getKeys(false)) {
            final List<String> commands = section.getStringList(group + ".commands");
            for (String command : commands) {
                this.commandGroups.put(command, group);
            }
        }
    }

    private void executeWebhook(Player player, String fullCommand) {
        final String group = this.commandGroups.get(fullCommand.split(" ")[0]);
        final String webhookUrl = this.getConfig().getString("groups." + group + ".webhook_url");

        if (webhookUrl == null) {
            this.getLogger().warning("Webhook URL null for group '" + group + "'");
            return;
        }

        final String body = this.applyBodyFormat(player, group, fullCommand);

        this.getLogger().info("This is the catastrophe I'm about to send: " + body);

        final HttpRequest request = HttpRequest.newBuilder()
                .header("User-Agent", this.getDescription() + "/" + this.getDescription().getVersion())
                .header("Content-Type", "application/json")
                .uri(URI.create(webhookUrl))
                .POST(BodyPublishers.ofString(body)).build();

        HttpResponse<String> response = null;
        try {
            response = this.client.send(request, BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            this.getLogger().warning("Failed to execute webhook: " + e.getMessage());
        }

        if (response == null) {
            return;
        }

        final int statusCode = response.statusCode();

        if (statusCode != 200) {
            this.getLogger().warning("Failed to execute webhook. Status Code: " + statusCode);
        }
    }
}
