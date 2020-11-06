package com.fsucsc.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
	public void onMessageReceived (MessageReceivedEvent event) {
		String rawMsg = event.getMessage()
			                 .getContentRaw();
		
		if (DisConfig.whitelistedUserId != 0) { //if a whitelist exists, enforce it
			if (event.getAuthor()
			         .getIdLong() != DisConfig.whitelistedUserId) {
				return;
			}
		}
		
		if (rawMsg.startsWith(Command.prefix)) {
			String command = null;
			String args = null;
			try {
				command = rawMsg.substring(Command.prefix.length(),
				                           rawMsg.indexOf(" ", Command.prefix.length()));
				args = rawMsg.substring(rawMsg.indexOf(" ",Command.prefix.length()) + 1);
			}
			catch (Exception ignored) {
				command = rawMsg.substring(Command.prefix.length());
				args = "";
			}

			//TODO(Michael): replace this with actual logging code.
			//Is just this actually sufficent?
			System.out.println("Command: " + command + "\nArgs: " + args);

			for (String id : DisConfig.blackListedUsers) {
				if (event.getAuthor().getId().equals(id)) {
					if (Command.BLACKLIST.name.equals(command)) { //Check if a blacklisted user is trying to use the blacklist.
						System.out.println("Special Blacklist Exception.");
						Command.BLACKLIST.action.execute(event, args);
					}

					return; //NOTE(Michael): Blacklisted users don't get past this point.
				}
			}
			
			for (Command cmd : Command.values()) {
				if (cmd.name.equals(command)) {
					cmd.action.execute(event, args);
				}
			}

		}
	}
}
