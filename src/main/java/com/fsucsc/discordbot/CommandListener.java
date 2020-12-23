package com.fsucsc.discordbot;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
	public void onMessageReceived (MessageReceivedEvent event) {
		String rawMsg = event.getMessage()
		                     .getContentRaw();

		if (DisConfig.WhitelistedUserId != 0) { //if a whitelist exists, enforce it
			if (event.getAuthor()
			         .getIdLong() != DisConfig.WhitelistedUserId) {
				return;
			}
		}

		if (rawMsg.startsWith(Command.prefix)) {
			String command = null;
			String args = null;
			try {
				command = rawMsg.substring(Command.prefix.length(),
				                           rawMsg.indexOf(" ", Command.prefix.length()));
				args = rawMsg.substring(rawMsg.indexOf(" ", Command.prefix.length()) + 1);
			}
			catch (Exception ignored) {
				command = rawMsg.substring(Command.prefix.length());
				args = "";
			}

			//TODO(Michael): replace this with actual logging code.
			//Is just this actually sufficient?
			System.out.println("Command: " + command + "   Args: " + args);

			for (String id : DisConfig.BlacklistedUsers) {
				if (event.getAuthor().getId().equals(id)) {
					if (Command.BLACKLIST.name.equals(command)) { //Check if a blacklisted user is trying to use the blacklist.
						System.out.println("Special Blacklist Exception.");
						Command.BLACKLIST.execute(event, args);
					}

					return; //NOTE(Michael): Blacklisted users don't get past this point.
				}
			}

			for (Command cmd : Command.values()) {
				if (cmd.name.equals(command)) {

					if (cmd.requiresPrivilegedRole) {
						Member author = event.getGuild().getMember(event.getAuthor());
						if (author.getRoles().contains(DisConfig.PrivilegedRole)) {
							cmd.execute(event, args);
						}
						else {
							Bot.SendMessage(event, "The command **" + command + "** requires the role " + DisConfig.PrivilegedRole.getAsMention() + ".");
						}
					}
					else {
						cmd.execute(event, args);
					}
					break; //we're only going to equal the name of one command

				}
			}

		}
	}
}
