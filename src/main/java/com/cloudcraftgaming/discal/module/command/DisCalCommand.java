package com.cloudcraftgaming.discal.module.command;

import com.cloudcraftgaming.discal.Main;
import com.cloudcraftgaming.discal.database.DatabaseManager;
import com.cloudcraftgaming.discal.internal.data.GuildSettings;
import com.cloudcraftgaming.discal.module.command.info.CommandInfo;
import com.cloudcraftgaming.discal.utils.ChannelUtils;
import com.cloudcraftgaming.discal.utils.Message;
import com.cloudcraftgaming.discal.utils.PermissionChecker;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;

/**
 * Created by Nova Fox on 1/5/2017.
 * Website: www.cloudcraftgaming.com
 * For Project: DisCal
 */
public class DisCalCommand implements ICommand {
    /**
     * Gets the command this Object is responsible for.
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "Discal";
    }

    /**
     * Gets the short aliases of the command this object is responsible for.
     * </br>
     * This will return an empty ArrayList if none are present
     *
     * @return The aliases of the command.
     */
    @Override
    public ArrayList<String> getAliases() {
        return new ArrayList<>();
    }

    /**
     * Gets the info on the command (not sub command) to be used in help menus.
     *
     * @return The command info.
     */
    @Override
    public CommandInfo getCommandInfo() {
        CommandInfo info = new CommandInfo("event");
        info.setDescription("Used to configure DisCal");
        info.setExample("!DisCal (function) (value)");

        info.getSubCommands().add("role");
        info.getSubCommands().add("channel");

        return info;
    }

    /**
     * Issues the command this Object is responsible for.
     * @param args The command arguments.
     * @param event The event received.
     * @param client The Client associated with the Bot.
     * @return <code>true</code> if successful, else <code>false</code>.
     */
    @Override
    public Boolean issueCommand(String[] args, MessageReceivedEvent event, IDiscordClient client) {
        IGuild guild = event.getMessage().getGuild();
        if (args.length < 1) {
          	EmbedBuilder em = new EmbedBuilder();
				em.withAuthorIcon(client.getGuildByID("266063520112574464").getIconURL());
				em.withAuthorName("DisCal!");
				em.withTitle("DisCal is the official Discord Calendar Bot!");
				em.appendField("Developer", "NovaFox161", true);
				em.appendField("Version", Main.version, true);
				em.appendField("Library", "Discord4J, version 2.7.0", false);
				em.appendField("Total Guilds", client.getGuilds().size() + "", true);
				em.appendField("Current Ping [Shard " + guild.getShard().getInfo()[0] + "]", guild.getShard().getResponseTime() + "ms", true);
				em.withFooterText("Be a patron today! https://www.patreon.com/Novafox");
				em.withUrl("https://www.cloudcraftgaming.com/discal/");
				em.withColor(36, 153, 153);
				Message.sendMessage(em.build(), event, client);
        } else if (args.length == 1) {
            Message.sendMessage("Please specify a function and value!", event, client);
        } else if (args.length == 2) {
            String function = args[0];
            if (function.equalsIgnoreCase("role")) {
                setControlRole(args, event, client);
            } else if (function.equalsIgnoreCase("channel")) {
                setChannel(args[1], event, client);
            } else {
                Message.sendMessage("Invalid function! Use `!help`", event, client);
            }
        } else if (args.length > 2) {
            Message.sendMessage("Invalid function! Use `!help`", event, client);
        }
        return false;
    }

    /**
     * Sets the control role for the guild.
     * @param args The args of the command.
     * @param event The event received.
     * @param client The Client associated with the Bot.
     */
    private void setControlRole(String[] args, MessageReceivedEvent event, IDiscordClient client) {
        if (PermissionChecker.hasSufficientRole(event)) {
            String roleName = args[1];
            IGuild guild = event.getMessage().getGuild();
            IRole controlRole = null;

            if (!"everyone".equalsIgnoreCase(roleName)) {
                for (IRole r : guild.getRoles()) {
                    if (r.getName().equals(roleName) || r.getID().equals(roleName)) {
                        controlRole = r;
                        break; //So that it only loops through a limited amount of roles.
                    }
                }

                if (controlRole != null) {
                    GuildSettings settings = DatabaseManager.getManager().getSettings(guild.getID());
                    settings.setControlRole(controlRole.getID());
                    DatabaseManager.getManager().updateSettings(settings);
                    //Send message.
                    Message.sendMessage("Required control role set to: `" + controlRole.getName() + "'", event, client);

                } else {
                    //Invalid role.
                    Message.sendMessage("Invalid role specified! The role must exist!", event, client);
                }
            } else {
                //Role is @everyone, set this so that anyone can control the bot.
                GuildSettings settings = DatabaseManager.getManager().getSettings(guild.getID());
                settings.setControlRole("everyone");
                DatabaseManager.getManager().updateSettings(settings);
                //Send message
                Message.sendMessage("Specific role no longer required! Everyone may edit/create!", event, client);
            }
        } else {
            Message.sendMessage("You do not have sufficient permissions to use this DisCal command!", event, client);
        }
    }

    /**
     * Sets the channel for the guild that DisCal can respond in.
     * @param channelName The name of the channel.
     * @param event The event received.
     * @param client The Client associated with the Bot.
     */
    private void setChannel(String channelName, MessageReceivedEvent event, IDiscordClient client) {
        if (channelName.equalsIgnoreCase("all")) {
            //Reset channel info.
            GuildSettings settings = DatabaseManager.getManager().getSettings(event.getMessage().getGuild().getID());
            settings.setDiscalChannel("all");
            DatabaseManager.getManager().updateSettings(settings);
            Message.sendMessage("DisCal will now respond in all channels!", event, client);
        } else {
            if (ChannelUtils.channelExists(channelName, event)) {
                IChannel channel = ChannelUtils.getChannelFromNameOrId(channelName, event);
                if (channel != null) {
                    GuildSettings settings = DatabaseManager.getManager().getSettings(event.getMessage().getGuild().getID());
                    settings.setDiscalChannel(channel.getID());
                    DatabaseManager.getManager().updateSettings(settings);
                    Message.sendMessage("DisCal will now only respond in channel: `" + channel.getName() + "`", event, client);
                } else {
                    Message.sendMessage("The specified channel does not exist!", event, client);
                }
            } else {
                Message.sendMessage("The specified channel does not exist!", event, client);
            }
        }
    }
}