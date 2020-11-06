package com.fsucsc.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
	public void onMessageReceived (MessageReceivedEvent event) {
		Message msg = event.getChannel()
		                   .retrieveMessageById(event.getMessageId())
		                   .complete();
		String rawMsg = msg.getContentRaw();

		//IMPORTANT(Michael): The ordering here is particular to evoke a certain behavior.
		//If a whitelist exists, we enforce it first and foremost. A whitelist should
		//only exist in a dev build of the bot.
		//Next, we want to process any !blacklist commands so that even if someone
		//is blacklisted they can unblacklist themselves. Blacklisting is intended to help
		//devs test out their changes by having the currently running version ignore them.
		//After that, we enforce the blacklist, so that devs who are working on their own
		//features won't interact with the current live version of the bot.
		//And finally, after the blacklist has been enforced, we process the message for the
		//rest of the commands.

		if (DisConfig.whitelistedUserId != 0) {
			if (!(msg.getAuthor()
			         .getIdLong() == DisConfig.whitelistedUserId)) {
				return; //BEGONE
			}
		}

		if (rawMsg.startsWith("!")) {
			String command = null;
			String args = null;
			try {
				command = rawMsg.substring(0, rawMsg.indexOf(" "));
				args = rawMsg.substring(rawMsg.indexOf(" ") + 1);
			}
			catch (Exception ignored) {
				command = rawMsg;
				args = "";
			}

			//NOTE(Michael): We *cannot* move this command; the blacklist command *must* be usable even while blacklisted
			//to function as intended.
			if (command.equals("!blacklist")) {
				try {
					if (args.isEmpty()) {
						StringBuilder blacklistedUsers = new StringBuilder();
						User tmpusr;
						JDA jda = msg.getJDA();
						for (String user : DisConfig.blackListedUsers) {
							tmpusr = jda.getUserById(user);
							blacklistedUsers.append(tmpusr.getName()).append("\n");
						}
						if (blacklistedUsers.length() == 0) {
							blacklistedUsers = new StringBuilder("None");
						}
						Bot.SendMessage(msg.getChannel(), "Blacklisted Users\n----------------\n" + blacklistedUsers);
					}
					else {
						if (!msg.getMentionedUsers().isEmpty()) {
							User usr = msg.getMentionedUsers().get(0);
							if (args.startsWith("add")) {
								if (DisConfig.blackListedUsers.contains(usr.getId())) {
									Bot.SendMessage(msg.getChannel(), usr.getName() + " is already blacklisted!");
								}
								else {
									DisConfig.blackListedUsers.add(usr.getId());
									Bot.SendMessage(msg.getChannel(), usr.getName() + " has been added to blacklist.");
								}
							}
							else if (args.startsWith("remove")) {
								if (!DisConfig.blackListedUsers.contains(usr.getId())) {
									Bot.SendMessage(msg.getChannel(), usr.getName() + " is not blacklisted!");
								}
								else {
									DisConfig.blackListedUsers.remove(usr.getId());
									Bot.SendMessage(msg.getChannel(), usr.getName() + " has been removed from blacklist.");
								}
							}
							else {
								Bot.SendMessage(msg.getChannel(), "Usage: `!blacklist <add|remove> <MentionedUser>`");
							}
						}
						else {
							Bot.SendMessage(msg.getChannel(), "No user mentioned!");
						}
					}
					return;
				}
				catch (Exception ex) {
					Bot.ReportStackTrace(ex, msg.getChannel());
				}
			}

			for (String id : DisConfig.blackListedUsers) {
				if (msg.getAuthor().getId().equals(id)) {
					return; //NOTE(Michael): Blacklisted users don't get past this point.
				}
			}
			/**************************/
			/* Old Style Above        */
			/**************************/
			command = command.substring(1);
			for (Command cmd : Command.values()) {
				if (cmd.name.equals(command)) {
					cmd.action.execute(event, args);
				}
			}

		}
	}
}
