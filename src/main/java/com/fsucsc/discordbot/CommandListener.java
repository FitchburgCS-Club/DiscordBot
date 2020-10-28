package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class CommandListener extends ListenerAdapter {
	String tmpDir = System.getProperty("java.io.tmpdir");
	EmbedBuilder builder = new EmbedBuilder();
	String attachmentName;
	Message.Attachment attachment;

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
			if (msg.getAuthor()
			       .getIdLong() == DisConfig.whitelistedUserId) {
				return;
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

			switch (command) {
				case "!ping":
					Bot.SendMessage(msg.getChannel(), "Pong!");
					break;
				case "!featurerequest":
					try (FileWriter fw = new FileWriter(DisConfig.outputDir + "FeatureRequests.txt", true)) {
						args = args.replace("\n", " ").trim();
						if (args.isEmpty()) {
							throw new IllegalArgumentException();
						}
						fw.write(args + "|" + msg.getAuthor().getId() + "\n");
						Bot.SendMessage(msg.getChannel(), "Submission \"" + args + "\" Received!");
					}
					catch (IllegalArgumentException ex) {
						Bot.SendMessage(msg.getChannel(), "Usage: `!featurerequest <Text Containing Your Feature Request>`");
					}
					catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
					break;
				case "!listrequests":
					try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.outputDir + "FeatureRequests.txt"))) {
						Stream<String> lines = br.lines();
						StringBuilder reply = new StringBuilder(2000);
						List<String> requestlist = lines.collect(Collectors.toList());
						for (int i = 1; i < requestlist.size() + 1; i++) {
							String[] parts = requestlist.get(i - 1).split("\\|");
							//NOTE(Michael): parts[0] == Content, parts[1] == Author
							parts[1] = msg.getGuild()
							              .getMemberById(parts[1])
							              .getEffectiveName();
							reply.append(i)
							     .append(". ")
							     .append(parts[0])
							     .append(" -- ")
							     .append(parts[1])
							     .append("\n");
						}
						//TODO(Michael): Streamline sending reply messages to commands, this just looks silly typing it all out again and again.
						if (reply.length() == 0) {
							Bot.SendMessage(msg.getChannel(), "There are no feature requests.");
						}
						else {
							Bot.SendMessage(msg.getChannel(), reply.toString());
						}
					}
					catch (FileNotFoundException ex) {
						Bot.SendMessage(msg.getChannel(), "No feature requests file found.");
					}
					catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
					break;
				case "!removerequest":
					int requestNum = Integer.parseInt(args) - 1;
					String oldRequest;
					try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.outputDir + "FeatureRequests.txt"))) {
						Stream<String> lines = br.lines();
						List<String> requestlist = lines.collect(Collectors.toList());
						try {
							oldRequest = requestlist.get(requestNum).split("\\|")[0];
							requestlist.remove(requestNum);
							BufferedWriter bw = new BufferedWriter(new FileWriter(DisConfig.outputDir + "FeatureRequests.txt"));
							for (String s : requestlist) {
								bw.write(s + "\n");
							}
							bw.flush();
							bw.close();
							Bot.SendMessage(msg.getChannel(), "Request \"" + oldRequest + "\" removed.");
						}
						catch (IndexOutOfBoundsException ex) {
							Bot.SendMessage(msg.getChannel(), "Invalid Request ID!");
						}
					}
					catch (FileNotFoundException ex) {
						Bot.SendMessage(msg.getChannel(), "No feature requests file found.");
					}
					catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
					break;
				case "!mackaystandard":
					builder.setColor(Color.RED)
					       .setImage("attachment://mackaystandard.jpeg")
					       .setTitle("Warning")
					       .setDescription("If you don't follow the Mackay standard, this could be you!");
					try {
						InputStream img = new FileInputStream("./mackaystandard.jpg");
						msg.getChannel()
						   .sendFile(img, "mackaystandard.jpg") //TODO(Michael): I think there's a way to include images in embeds? might be a cool edit.
						   .embed(builder.build())
						   .queue();
					}
					catch (FileNotFoundException ex) {
						Bot.SendMessage(msg.getChannel(), "Error:\nImage not found on server.");
					}
					catch (Exception ex) {
						Bot.ReportStackTrace(ex, msg.getChannel());
					}
				case "!blur":
					attachment = msg.getAttachments().get(0);
					attachmentName = attachment.getFileName();
					if (attachment.isImage()) {
						// (Zack) You have to use "thenAccept" to make sure the file is
						// actually done uploading before you do more stuff

						// variables used in lambdas must be final
						final String finalArgs = args;
						attachment.downloadToFile(tmpDir + "/" + attachmentName).thenAccept(file->{
							try {
								Double blurAmnt;
								try {
									blurAmnt = Double.parseDouble(finalArgs);
								}
								catch (Exception ex) {
									Bot.SendMessage(msg.getChannel(), "Unable to parse blur amount, defaulting to '2.0'");
									blurAmnt = 2.0;
								}
								ConvertCmd cmd = new ConvertCmd();
								IMOperation op = new IMOperation();
								op.addImage(tmpDir + "/" + attachmentName);
								op.blur(0.0, blurAmnt);
								op.addImage(tmpDir + "/blur_" + attachmentName);
								cmd.run(op);
								File f = new File(tmpDir + "/blur_" + attachmentName);
								builder.setColor(Color.CYAN);
								builder.setImage("attachment://blur" + attachmentName);
								try {
									InputStream img = new FileInputStream(tmpDir + "/blur_" + attachmentName);
									msg.getChannel().sendFile(img, attachmentName).embed(builder.build()).queue();
								}
								catch (FileNotFoundException ex) {
									Bot.SendMessage(msg.getChannel(), "Error:\nImage not found on server.");
								}
								catch (Exception ex) {
									StringWriter sw = new StringWriter();
									ex.printStackTrace(new PrintWriter(sw));
									Bot.SendMessage(msg.getChannel(), "An unexpected exception occurred! Here's the info:\n" + sw.toString());
								}
								f.delete();
							}
							catch (Exception ex) {
								StringWriter sw = new StringWriter();
								ex.printStackTrace(new PrintWriter(sw));
								Bot.SendMessage(msg.getChannel(), "Failed to convert image Here's the info:\n" + sw.toString());
							}
						}).exceptionally(ex->{
							StringWriter sw = new StringWriter();
							ex.printStackTrace(new PrintWriter(sw));
							Bot.SendMessage(msg.getChannel(), "Failed to download image Here's the info:\n" + sw.toString());
							return null;
						});
					}
					else {
						Bot.SendMessage(msg.getChannel(), "Error:\nEither no image was attached or attachment is not an image!");
					}
					break;
				default:
					Bot.SendMessage(msg.getChannel(), "Unknown Command!");
			}
		}
	}
}