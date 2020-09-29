package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandListener extends ListenerAdapter {
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		Message msg = event.getChannel().retrieveMessageById(event.getMessageId()).complete();
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

		if (DisConfig.whitelistedUser != null) {
			if (!msg.getAuthor().getId().equals(DisConfig.whitelistedUser)) {
				return;
			}
		}

		if (rawMsg.startsWith("!")) {
			String command = "";
			String args = "";
			try {
				command = rawMsg.substring(0, rawMsg.indexOf(" "));
				args = rawMsg.substring(rawMsg.indexOf(" ") + 1);
			} catch (Exception ignored) {
				command = rawMsg;
				args = "";
			}

			//NOTE(Micahel): We *cannot* move this command; the blacklist command *must* be usable even while blacklisted
			//to function as intended.
			if (command.equals("!blacklist")) {
					if (args.isEmpty()) {
						String blacklistedUsers = "";
						User tmpusr;
						JDA jda = msg.getJDA();
						for (String user : DisConfig.blackListedUsers) {
							tmpusr = jda.getUserById(user);
							blacklistedUsers += tmpusr.getName() + "\n";
						}
						if (blacklistedUsers.isEmpty()) {
							blacklistedUsers = "None";
						}
						Bot.SendMessage(msg.getChannel(), "Blacklisted Users\n----------------\n" + blacklistedUsers);
					} else {
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
							} else if (args.startsWith("remove")) {
								if (!DisConfig.blackListedUsers.contains(usr.getId())) {
									Bot.SendMessage(msg.getChannel(), usr.getName() + " is not blacklisted!");
								}
								else {
									DisConfig.blackListedUsers.remove(usr.getId());
									Bot.SendMessage(msg.getChannel(), usr.getName() + " has been removed from blacklist.");
								}
							} else {
								Bot.SendMessage(msg.getChannel(), "Usage: `!blacklist <add|remove> <MentionedUser>`");
							}
						} else {
							Bot.SendMessage(msg.getChannel(), "No user mentioned!");
						}
					}
				return;
			}

			for (String id : DisConfig.blackListedUsers) {
				if (msg.getAuthor().getId().equals(id)) {
					return; //NOTE(Michael): Blacklisted users don't get past this point.
				}
			}

			switch (command) {
				case "!ping":
					Bot.SendMessage(msg.getChannel(), "Pong!");
					break;
				case "!featurerequest":
					try (FileWriter fw = new FileWriter(DisConfig.outDir + "FeatureRequests.txt", true)) {
						args = args.replace("\n", " ").trim();
						if (args.isEmpty()) {
							throw new IllegalArgumentException();
						}
						fw.write(args + "|" + msg.getAuthor().getId() + "\n");
						Bot.SendMessage(msg.getChannel(), "Submission \"" + args + "\" Received!");
					} catch (IllegalArgumentException ex) {
						Bot.SendMessage(msg.getChannel(), "Usage: `!featurerequest <Text Containing Your Feature Request>`");
					} catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
					break;
				case "!listrequests":
					try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.outDir + "FeatureRequests.txt"))) {
						Stream<String> lines = br.lines();
						StringBuilder reply = new StringBuilder(2000);
						List<String> linelist = lines.collect(Collectors.toList());
						for (int i = 1; i < linelist.size() + 1; i++) {
							String[] parts = linelist.get(i - 1).split("\\|");
							//NOTE(Michael): parts[0] == Content, parts[1] == Author
							parts[1] = msg.getGuild().getMemberById(parts[1]).getEffectiveName();
							reply.append(i).append(". ").append(parts[0])
									.append(" -- ").append(parts[1]).append("\n");
						}
						//TODO(Michael): Streamline sending reply messages to commands, this just looks silly typing it all out again and again.
						if (reply.length() == 0) {
							Bot.SendMessage(msg.getChannel(), "There are no feature requests.");
						} else {
							Bot.SendMessage(msg.getChannel(), reply.toString());
						}
					} catch (FileNotFoundException ex) {
						Bot.SendMessage(msg.getChannel(), "No feature requests file found.");
					} catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
					break;
				case "!mackaystandard":
					//TODO(Michael): I think there's a way to include images in embeds? might be a cool edit.
					EmbedBuilder builder = new EmbedBuilder();
					builder.setColor(Color.RED);
					builder.setImage("attachment://mackaystandard.jpeg");
					builder.setTitle("Warning");
					builder.setDescription("If you don't follow the Mackay standard, this could be you!");

					try {
						InputStream img = new FileInputStream("./mackaystandard.jpg");
						msg.getChannel().sendFile(img, "mackaystandard.jpg").embed(builder.build()).queue();
					} catch (FileNotFoundException ex) {
						Bot.SendMessage(msg.getChannel(), "Error:\nImage not found on server.");
					} catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
				default:
					Bot.SendMessage(msg.getChannel(), "Unknown Command!");
			}
		}
	}
}
