package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TODO(Michael): Explain why this works. Also find out why this works.
 * This interface is a nicely shaped box that we can shove function references into.
 * And that's just what we'll do!
 */
interface ICommandAction {
	void execute (MessageReceivedEvent event, String args);
}

/* template for new command fucntion

  public static void  (MessageReceivedEvent event, String args) {
  }
 */


class CommandAction {
	public static void ping (MessageReceivedEvent event, String args) {
		Bot.SendMessage(event.getChannel(), "Pong!");
	}

	public static void featureRequest (MessageReceivedEvent event, String args) {
		Message msg = event.getChannel()
		                   .retrieveMessageById(event.getMessageId())
		                   .complete();

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
	}

	public static void listRequests (MessageReceivedEvent event, String args) {
		Message msg = event.getChannel()
		                   .retrieveMessageById(event.getMessageId())
		                   .complete();
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
				//TODO(Michael): This function will will throw a pointer execption if the user who requested something has left the server.
				//We ought to create a error handler for this.
				reply.append(i)
				     .append(". ")
				     .append(parts[0])
				     .append(" -- ")
				     .append(parts[1])
				     .append("\n");
			}
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
	}

	public static void removeRequest (MessageReceivedEvent event, String args) {
		Message msg = event.getChannel()
		                   .retrieveMessageById(event.getMessageId())
		                   .complete();
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
				Bot.SendMessage(msg.getChannel(), "Invalid Request index!");
			}
		}
		catch (FileNotFoundException ex) {
			Bot.SendMessage(msg.getChannel(), "No feature requests file found.");
		}
		catch (Exception ex) {
			Bot.ReportStackTrace(ex, msg.getChannel());
		}
	}

	public static void mackayStandard (MessageReceivedEvent event, String args) {
		Message msg = event.getChannel()
		                   .retrieveMessageById(event.getMessageId())
		                   .complete();
		EmbedBuilder builder = new EmbedBuilder();
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
	}

	public static void blacklist (MessageReceivedEvent event, String args) {
		try {
			if (args.isEmpty()) {
				StringBuilder blacklistedUsers = new StringBuilder();
				for (String user : DisConfig.blackListedUsers) {
					blacklistedUsers.append( event.getJDA().getUserById(user).getName() )
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
			return;
		}
		catch (Exception ex) {
			Bot.ReportStackTrace(ex, event.getChannel());
		}
	}
	
	//TODO: test.
	public static void help (MessageReceivedEvent event, String args) {
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

	public static void dummy (MessageReceivedEvent event, String args) {
		Bot.SendMessage(event.getChannel(), "This command doesn't exist yet!");
	}
}


public enum Command {
	PING("ping", "",
	     "Pings the bot, causing it to pong.",
	     CommandAction::ping),

	BLACKLIST("blacklist", "[add|remove] [MentionedUser]",
	          "This command will prevent the bot from accepting commands from a user.\n" +
	          "Useful for developers so they can test a local version while avoiding the current live version.\n" +
	          "`!blacklist add` adds the mentioned user to the blacklist.\n" +
	          "`!blacklist remove` removes the mentioned user from the blacklist.\n" +
	          "`!blacklist` lists out the currently blacklisted users.",
	          CommandAction::blacklist),

	FEATURE_REQUEST("featureRequest", "<Request>",
	                "Request a feature to be added to the bot.\n" +
	                "Everything after the command name is intrepreted as part of the request.",
	                CommandAction::featureRequest),

	LIST_REQUESTS("listRequests", "",
	              "List current requests for the bot.",
	              CommandAction::listRequests),

	REMOVE_REQUEST("removeRequest", "<Index>",
	               "Removes a request from the feature request list\n" +
	               "Use `!listRequests` to find the index of a feature request",
	               CommandAction::removeRequest),

	MACKAY_STANDARD("mackayStandard", "",
	                "Let's you know what happens when you don't follow Mackay Standards.",
	                CommandAction::mackayStandard),

	HELP("help", "[CommandName]",
	     "The command that you're looking at now. XD\n",
	     CommandAction::help);

	static String prefix = "!";

	final String name; ///Name of the Command
	final private String params; ///The params the command takes, only used for !help
	final private String desc; ///The desc of the command, only used for !help
	final ICommandAction action; ///Used to call the action of the command.

	Command (String name, String params, String desc, ICommandAction action) {
		this.name = name;
		this.params = params;
		this.desc = desc;
		this.action = action;
	}

	final String getHelpString () {
		return
				name + ":\n" +
				"Syntax `" + prefix + name + " " + params + "`\n" +
				desc;
	}
}
