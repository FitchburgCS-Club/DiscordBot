package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.entities.MessageChannel;

import java.awt.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;

class MeetingNotif implements Runnable {
	String message;
	MessageChannel sendChannel;	

	public MeetingNotif(String newMessage, MessageChannel newSendChannel) {
		message = newMessage;
		sendChannel = newSendChannel;
	}
	
	@Override
	public void run() {
		Bot.SendMessage(sendChannel, "@ everyone " + message);
	}
}

public enum Command {
	PING("ping", "",
	     "The father of all commands.\n" +
	     "Pings the bot, causing it to pong.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			Bot.SendMessage(event.getChannel(), "Pong!");
		}
	},

	HELP("help", "[CommandName]",
	     "The command that you're looking at now. XD\n" +
	     "Commands that have `[params]` formatted like that are optional parameters.\n" +
	     "Commands that have `<params>` formatted like that are required parameters.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			String reply = "Command \"" + args + "\" does not exist!";

			if (args.isEmpty()) { //print all help
				StringBuilder strBld = new StringBuilder(1000);
				for (Command cmd : Command.values()) {
					strBld.append(cmd.getHelpString())
					      .append("\n\n");
				}
				reply = strBld.toString();
			}
			else { //print particular help
				for (Command cmd : Command.values()) {
					if (cmd.name.equals(args)) {
						reply = cmd.getHelpString();
						break; //we're done here.
					}
				}
			}
			Bot.SendMessage(event.getChannel(), reply);
		}
	},

	BLACKLIST("blacklist", "[add|remove] [MentionedUser]",
	          "This command will prevent the bot from accepting commands from a user.\n" +
	          "Useful for developers so they can test a local version while avoiding the current live version.\n" +
	          "`!blacklist add` adds the mentioned user to the blacklist.\n" +
	          "`!blacklist remove` removes the mentioned user from the blacklist.\n" +
	          "`!blacklist` lists out the currently blacklisted users.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			try {
				if (args.isEmpty()) {
					StringBuilder blacklistedUsers = new StringBuilder();
					for (String user : DisConfig.blackListedUsers) {
						blacklistedUsers.append(event.getJDA().getUserById(user).getName())
						                .append("\n");
					}
					if (blacklistedUsers.length() == 0) {
						blacklistedUsers.append("None");
					}
					Bot.SendMessage(event.getChannel(), "Blacklisted Users\n----------------\n" + blacklistedUsers);
				}
				else {
					if (!event.getMessage().getMentionedUsers().isEmpty()) {
						User usr = event.getMessage().getMentionedUsers().get(0);
						if (args.startsWith("add")) {
							if (DisConfig.blackListedUsers.contains(usr.getId())) {
								Bot.SendMessage(event.getChannel(), usr.getName() + " is already blacklisted!");
							}
							else {
								DisConfig.blackListedUsers.add(usr.getId());
								Bot.SendMessage(event.getChannel(), usr.getName() + " has been added to blacklist.");
							}
						}
						else if (args.startsWith("remove")) {
							if (!DisConfig.blackListedUsers.contains(usr.getId())) {
								Bot.SendMessage(event.getChannel(), usr.getName() + " is not blacklisted!");
							}
							else {
								DisConfig.blackListedUsers.remove(usr.getId());
								Bot.SendMessage(event.getChannel(), usr.getName() + " has been removed from blacklist.");
							}
						}
						else {
							Bot.SendMessage(event.getChannel(), "Usage: `!blacklist <add|remove> <MentionedUser>`");
						}
					}
					else {
						Bot.SendMessage(event.getChannel(), "No user mentioned!");
					}
				}
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(ex, event.getChannel());
			}
		}
	},

	FEATURE_REQUEST("featureRequest", "<Request>",
	                "Request a feature to be added to the bot.\n" +
	                "Everything after the command name is interpreted as part of the request.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			try (FileWriter fw = new FileWriter(DisConfig.outputDir + "FeatureRequests.txt", true)) {
				args = args.replace("\n", " ").trim();
				if (args.isEmpty()) {
					throw new IllegalArgumentException();
				}
				fw.write(args + "|" + event.getAuthor().getId() + "\n");
				Bot.SendMessage(event, "Submission \"" + args + "\" Received!");
			}
			catch (IllegalArgumentException ex) {
				Bot.SendMessage(event, "Usage: `!featurerequest <Text Containing Your Feature Request>`");
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(ex, event.getChannel());
			}
		}
	},

	LIST_REQUESTS("listRequests", "",
	              "List current requests for the bot.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.outputDir + "FeatureRequests.txt"))) {
				Stream<String> lines = br.lines();
				StringBuilder reply = new StringBuilder(2000);
				List<String> requestlist = lines.collect(Collectors.toList());
				for (int i = 1; i < requestlist.size() + 1; i++) {
					String[] parts = requestlist.get(i - 1).split("\\|");
					//NOTE(Michael): parts[0] == Content, parts[1] == Author
					parts[1] = event.getGuild()
					                .getMemberById(parts[1])
					                .getEffectiveName();
					//TODO(Michael): This function will will throw a null pointer exception if the user who requested something has left the server.
					//We ought to create a error handler for this.
					reply.append(i)
					     .append(". ")
					     .append(parts[0])
					     .append(" -- ")
					     .append(parts[1])
					     .append("\n");
				}
				if (reply.length() == 0) {
					Bot.SendMessage(event, "There are no feature requests.");
				}
				else {
					Bot.SendMessage(event, reply.toString());
				}
			}
			catch (FileNotFoundException ex) {
				Bot.SendMessage(event, "No feature requests file found.");
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(ex, event.getChannel());
			}
		}
	},

	REMOVE_REQUEST("removeRequest", "<Index>",
	               "Removes a request from the feature request list\n" +
	               "Use `!listRequests` to find the index of a feature request") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
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
					Bot.SendMessage(event, "Request \"" + oldRequest + "\" removed.");
				}
				catch (IndexOutOfBoundsException ex) {
					Bot.SendMessage(event, "Invalid Request index!");
				}
			}
			catch (FileNotFoundException ex) {
				Bot.SendMessage(event, "No feature requests file found.");
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(ex, event.getChannel());
			}
		}
	},

	MACKAY_STANDARD("mackayStandard", "",
	                "Let's you know what happens when you don't follow Mackay Standards.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(Color.RED)
			       .setImage("attachment://mackaystandard.jpeg")
			       .setTitle("Warning")
			       .setDescription("If you don't follow the Mackay standard,, this could be you!");
			try {
				InputStream img = new FileInputStream("./mackaystandard.jpg");
				event.getChannel()
				     .sendFile(img, "mackaystandard.jpg") //TODO(Michael): I think there's a way to include images in embeds? might be a cool edit.
				     .embed(builder.build())
				     .queue();
			}
			catch (FileNotFoundException ex) {
				Bot.SendMessage(event, "Error:\nImage not found on server.");
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(ex, event.getChannel());
			}
		}
	},

	//Note(Michael): This command is DANGEROUS AND ARMED. if you are testing this, please change the '@everyone' in the MeetingNotif class to something else.
	SCHEDULE_MEETING("scheduleMeeting", "<yyyy-MM-dd HH:mm> | <Text>",
	                 "Shows a reminder when a upcoming meeting is about to begin.\n" +
	                 "`<yyyy-MM-dd HH:mm>` refers to the date and time at which the meeting will start.\n" +
	                 "y stands for year, M stands for month, d stands for day, H stands for hour in 24 hr format where '0' is midnight\n" +
	                 "m stands for minute.\n" +
	                 "`<text>` is the text that will show when the reminder sends.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			String strDate = args.substring(0, args.indexOf("|"));
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			try {
				long meetingTime = df.parse(strDate).getTime();
				long curTime = new Date().getTime();
				long meetingDelta = meetingTime - curTime;

				if (meetingDelta <= 0) {
					Bot.SendMessage(event, "The supplied date is in the past!");
					return;
				}

				String message = args.substring(args.indexOf("|") + 2); //Note(Michael): to consume the ' ' after the pipe.
				
				Bot.TaskScheduler.schedule(new MeetingNotif(message, event.getChannel()), meetingDelta, TimeUnit.MILLISECONDS);
			}
			catch (ParseException ex) {
				Bot.SendMessage(event, "The supplied date was invalid!\nSubmit your date in 'yyyy-MM-dd HH:mm' format");
			}
			catch (IndexOutOfBoundsException ex) {
				Bot.SendMessage(event, "The `<Text>` parameter was invalid");
			}
			catch (Exception ex) { Bot.ReportStackTrace(ex, event.getChannel()); }
		}
	};

	static String prefix = "!"; //The prefix for all commands.

	final public String name; //Name of the Command
	final private String params; //The params the command takes, only used for !help
	final private String desc; //The desc of the command, only used for !help

	Command (String name, String params, String desc) {
		this.name = name;
		this.params = params;
		this.desc = desc;
	}

	public String getHelpString () {
		return
				name + ":\n" +
				"Syntax `" + prefix + name + " " + params + "`\n" +
				desc;
	}

	/**
	 * Represents the action a command does when it is invoked
	 *
	 * @param event The message event that triggered this command
	 * @param args The text that came after the command name
	 * <p>
	 * All commands that we create must override this method in a anonymous class belonging to the enum entry.
	 * For a easy example to look at, check out the entry for PING.
	 */
	abstract public void execute (MessageReceivedEvent event, String args);
}
