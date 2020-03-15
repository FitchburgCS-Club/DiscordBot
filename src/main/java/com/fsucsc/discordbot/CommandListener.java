package com.fsucsc.discordbot;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.FileWriter;
import java.io.IOException;

public class CommandListener extends ListenerAdapter {
	//NOTE(Michael):we use onGenericMessage to allow for command testing in PMs.
	public void onGenericMessage (GenericMessageEvent e) {
		Message msg = e.getChannel().retrieveMessageById(e.getMessageId()).complete ();
		String rawMsg = msg.getContentRaw();

		if (rawMsg.startsWith("!ping")) {
				msg.getChannel().sendMessage("Pong!").queue ();
		}

		if (rawMsg.startsWith("!featurerequest")) {
			try {
				FileWriter fw = new FileWriter(DisConfig.outDir + "FeatureRequests.txt", true);
				String message = msg.getContentStripped();
				message = message.replace("\n", " ");
				fw.write(message.substring(16) + "\n");
				fw.close();
				msg.getChannel().sendMessage("Submission Received!").queue();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}
}
