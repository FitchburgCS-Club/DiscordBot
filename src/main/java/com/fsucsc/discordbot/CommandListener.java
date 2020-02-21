package com.fsucsc.discordbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class CommandListener extends ListenerAdapter {
	//NOTE(Michael):we use onGenericMessage to allow for command testing in PMs.
	public void onGenericMessage (GenericMessageEvent e) {
		Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete ();
		String rawMsg = msg.getContentRaw();

		if (DisConfig.whitelistedUser != null) {
			if (!msg.getAuthor().getId().equals(DisConfig.whitelistedUser)) {
				return;
			}
		}

		if (rawMsg.startsWith("!ping")) {
				msg.getChannel().sendMessage("Pong!").queue ();
		}

		//TODO(Michael): Add more commands here...
	}
}
