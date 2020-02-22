package com.fsucsc.discordbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
	//NOTE(Michael): we use onGenericMessage to allow for command testing in PMs.
	public void onGenericMessage (GenericMessageEvent e) {
		Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete ();
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
			rawMsg = rawMsg.substring("!blacklist".length() + 1);
			User usr = msg.getMentionedUsers().get(0);
			if (rawMsg.startsWith ("add")) {
				DisConfig.blackListedUsers.add (usr.getId());
			}
			else if (rawMsg.startsWith ("remove")) {
				DisConfig.blackListedUsers.remove (usr.getId());
			}
		}

		for (String id : DisConfig.blackListedUsers) {
			if (msg.getAuthor().getId().equals(id)) {
				return;
			}
		}

		if (rawMsg.startsWith("!ping")) {
				msg.getChannel().sendMessage("Pong!").queue ();
		}

		//TODO(Michael): Add more commands here...
	}
}
