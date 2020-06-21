package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.EmbedType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.Color;
import java.io.*;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

		if (rawMsg.startsWith("!")) {
			String command = rawMsg.split(" ")[0].strip();

			switch (command) {
				case "!blacklist":
					try {
						rawMsg = rawMsg.substring("!blacklist ".length());
						User usr = msg.getMentionedUsers().get(0);
						if (rawMsg.startsWith("add")) {
							DisConfig.blackListedUsers.add(usr.getId());
						} else if (rawMsg.startsWith("remove")) {
							DisConfig.blackListedUsers.remove(usr.getId());
						} else {
							throw new Exception("Invalid Argument");
						}
					} catch (Exception ex) {
						msg.getChannel().sendMessage("Usage: `!blacklist <add|remove> <MentionedUser>`").queue();
					}
					break;
				case "!ping":
					msg.getChannel().sendMessage("Pong!").queue();
					break;
				case "!featurerequest":
					try (FileWriter fw = new FileWriter(DisConfig.outDir + "FeatureRequests.txt", true)) {
						String message = msg.getContentStripped()
								.substring("!featurerequest ".length())
								.replace("\n", " ")
								.trim();
						if (message.isEmpty()) {
							throw new Exception("Invalid Argument");
						}
						fw.write(message + "|" + msg.getAuthor().getName() + "\n");
						msg.getChannel().sendMessage("Submission \"" + message + "\" Received!").queue();
					} catch (Exception ex) {
						msg.getChannel().sendMessage("Fatal Error.\n" + ex + "\nShow this to a programmer.").queue();
					}
					break;
				case "!listrequests":
					try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.outDir + "FeatureRequests.txt"))) {
						Stream<String> lines = br.lines();
						String message = "";
						ArrayList linelist = (ArrayList) lines.collect(Collectors.toList());
						int i = 1;
						for (Object l : linelist) {
							// for loop doesn't want to take a string for some reason so we have to use a plain object and cast to string.
							String r = (String)l;
							String[] parts = r.split("\\|");
							message += i+". "+parts[0]+" -- "+parts[1]+"\n";
							i++;
						}
						msg.getChannel().sendMessage(message).queue();
					} catch (FileNotFoundException ex) {
						msg.getChannel().sendMessage("No feature requests file found.").queue();
					} catch (Exception ex) {
						msg.getChannel().sendMessage("Fatal Error.\n" + ex + "\nShow this to a programmer.").queue();
					}
					break;
				case "!mackaystandard":
					EmbedBuilder builder = new EmbedBuilder();
					builder.setColor(Color.RED);
					builder.setImage("attachment://mackaystandard.jpeg");
					builder.setTitle("Warning");
					builder.setDescription("If you don't follow the Mackay standard, this could be you!");

					try {
						InputStream img = new FileInputStream("./mackaystandard.jpg");
						msg.getChannel().sendFile(img, "mackaystandard.jpg").embed(builder.build()).queue();
					} catch (FileNotFoundException ex) {
						msg.getChannel().sendMessage("Error:\nImage not found on server.").queue();
					}
				default:
					msg.getChannel().sendMessage("Unknown Command!").queue();
			}
		}

		for (String id : DisConfig.blackListedUsers) {
			if (msg.getAuthor().getId().equals(id)) {
				return;
			}
		}
	}
}
