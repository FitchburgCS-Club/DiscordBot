package com.fsucsc.discordbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.Exception;

public class CommandListener extends ListenerAdapter {
	//NOTE(Michael): we use onGenericMessage to allow for command testing in PMs.
	public void onGenericMessage (GenericMessageEvent event) {
		Message msg = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
		String rawMsg = msg.getContentRaw();

		//IMPORTANT(Michael): The ordering here is perticular to evoke a certain behaivor.
		//If a whitelist exists, we enforce it first and formost. A whitelist should
		//only exist in a dev build of the bot.
		//Next, we want to process any !blacklist commands so that even if someone
		//is blacklisted they can unblacklist themselves. Blacklisting is intended to help
		//devs test out their changes by having the currently running version ignore them.
		//After that, we enforce the blacklist, so that devs who are working on their own
		//features won't interact with the current live version of the bot.
		//And finally, after the blacklist has been enforced, we process the message for the
		//rest of the commands.

		if (DisConfig.whitelistedUser != null) {
			if (!msg.getAuthor().getId().equals(DisConfig.whitelistedUser)) {
				return;
			}
		}

		if (rawMsg.startsWith("!blacklist")) {
			try {
				rawMsg = rawMsg.substring("!blacklist ".length());
				User usr = msg.getMentionedUsers().get(0);
				if (rawMsg.startsWith("add")) {
					DisConfig.blackListedUsers.add(usr.getId());
				} else if (rawMsg.startsWith("remove")) {
					DisConfig.blackListedUsers.remove(usr.getId());
				} else {
					throw new Exception("Invalid Arguement");
				}
			} catch (Exception ex) {
				msg.getChannel().sendMessage("Usage: `!blacklist <add|remove> <MentionedUser>`").queue();
			}
		}

		for (String id : DisConfig.blackListedUsers) {
			if (msg.getAuthor().getId().equals(id)) {
				return;
			}
		}

		if (rawMsg.startsWith("!ping")) {
			msg.getChannel().sendMessage("Pong!").queue();
		}

		if (rawMsg.startsWith("!featurerequest")) {
			try (FileWriter fw = new FileWriter(DisConfig.outDir + "FeatureRequests.txt", true);) {
				String message = msg.getContentStripped()
				                    .substring("!featurerequest ".length())
				                    .replace("\n", " ")
				                    .trim();
				if (message.isEmpty()) {
					throw new Exception ("Invalid Arguement");
				}
				fw.write(message + "\n");
				msg.getChannel().sendMessage("Submission \"" + message + "\" Received!").queue();
			} catch (IOException ex) {
				msg.getChannel().sendMessage("Fatal Error.\n" + ex + "\nShow this to a programmer.").queue();
			} catch (Exception ex) {
				msg.getChannel().sendMessage("Usage: `!featurerequest <Request>`").queue();
			}
		}
	}
}
