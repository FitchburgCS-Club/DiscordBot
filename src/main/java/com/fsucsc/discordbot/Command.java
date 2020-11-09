package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This is a known as a functional interface since it only has one abstract method.
 * <p>
 * Because it is a functional interface, any method that has the same prototype as the
 * function in this interface "implements" this interface.
 * <p>
 * That is to say, if we have a variable of this interface, we can put a function reference
 * into that variable as long as the function in question has the same prototype as ICommandAction.execute().
 */
interface ICommandAction {
	void execute (MessageReceivedEvent event, String args);
}

/* template for new command function

  public static void  (MessageReceivedEvent event, String args) {
  }
 */


class CommandAction {
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

	public static void ping (MessageReceivedEvent event, String args) {
		Bot.SendMessage(event.getChannel(), "Pong!");
	}

	public static void featureRequest (MessageReceivedEvent event, String args) {
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

	public static void listRequests (MessageReceivedEvent event, String args) {
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

	public static void removeRequest (MessageReceivedEvent event, String args) {
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

	public static void mackayStandard (MessageReceivedEvent event, String args) {
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

	public static void blacklist (MessageReceivedEvent event, String args) {
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

	public static void blur (MessageReceivedEvent event, String args) {
		String tmpDir = System.getProperty("java.io.tmpdir");
		EmbedBuilder builder = new EmbedBuilder();
		String attachmentName;
		Message.Attachment attachment;

		attachment = event.getMessage().getAttachments().get(0);
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
						event.getChannel().sendFile(img, attachmentName).embed(builder.build()).queue();
					}
					catch (FileNotFoundException ex) {
						Bot.SendMessage(event.getChannel(), "Error:\nImage not found on server.");
					}
					catch (Exception ex) {
						StringWriter sw = new StringWriter();
						ex.printStackTrace(new PrintWriter(sw));
						Bot.SendMessage(event.getChannel(), "An unexpected exception occurred! Here's the info:\n" + sw.toString());
					}
					f.delete();
				}
				catch (Exception ex) {
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));
					Bot.SendMessage(event.getChannel(), "Failed to convert image Here's the info:\n" + sw.toString());
				}
			}).exceptionally(ex->{
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				Bot.SendMessage(event.getChannel(), "Failed to download image Here's the info:\n" + sw.toString());
				return null;
			});
		}
		else {
			Bot.SendMessage(event.getChannel(), "Error:\nEither no image was attached or attachment is not an image!");
		}
	}

	public static void mblur (MessageReceivedEvent event, String args) {
		String tmpDir = System.getProperty("java.io.tmpdir");
		EmbedBuilder builder = new EmbedBuilder();
		String attachmentName;
		Message.Attachment attachment;

		attachment = event.getMessage().getAttachments().get(0);
		attachmentName = attachment.getFileName();
		if (attachment.isImage()) {
			// (Zack) You have to use "thenAccept" to make sure the file is
			// actually done uploading before you do more stuff

			// variables used in lambdas must be final
			final String[] finalArgs = args.split(" ");
			attachment.downloadToFile(tmpDir + "/" + attachmentName).thenAccept(file->{
				try {
					Double blurAmnt;
					Double blurAmnt2;
					try {
						blurAmnt = Double.parseDouble(finalArgs[0]);
						blurAmnt2 = Double.parseDouble(finalArgs[1]);
					}
					catch (Exception ex) {
						blurAmnt = 10.0;
						blurAmnt2 = 45.0;
					}
					ConvertCmd cmd = new ConvertCmd();
					IMOperation op = new IMOperation();
					op.addImage(tmpDir + "/" + attachmentName);
					op.motionBlur(0.0, blurAmnt, blurAmnt2);
					op.addImage(tmpDir + "/blur_" + attachmentName);
					cmd.run(op);
					File f = new File(tmpDir + "/blur_" + attachmentName);
					builder.setColor(Color.CYAN);
					builder.setImage("attachment://blur" + attachmentName);
					try {
						InputStream img = new FileInputStream(tmpDir + "/blur_" + attachmentName);
						event.getChannel().sendFile(img, attachmentName).embed(builder.build()).queue();
					}
					catch (FileNotFoundException ex) {
						Bot.SendMessage(event.getChannel(), "Error:\nImage not found on server.");
					}
					catch (Exception ex) {
						StringWriter sw = new StringWriter();
						ex.printStackTrace(new PrintWriter(sw));
						Bot.SendMessage(event.getChannel(), "An unexpected exception occurred! Here's the info:\n" + sw.toString());
					}
					f.delete();
				}
				catch (Exception ex) {
					StringWriter sw = new StringWriter();
					ex.printStackTrace(new PrintWriter(sw));
					Bot.SendMessage(event.getChannel(), "Failed to convert image Here's the info:\n" + sw.toString());
				}
			}).exceptionally(ex->{
				StringWriter sw = new StringWriter();
				ex.printStackTrace(new PrintWriter(sw));
				Bot.SendMessage(event.getChannel(), "Failed to download image Here's the info:\n" + sw.toString());
				return null;
			});
		}
		else {
			Bot.SendMessage(event.getChannel(), "Error:\nEither no image was attached or attachment is not an image!");
		}
	}

	public static void dummy (MessageReceivedEvent event, String args) {
		Bot.SendMessage(event.getChannel(), "This command doesn't exist yet!");
	}
}


public enum Command {
	HELP("help", "[CommandName]",
	     "The command that you're looking at now. XD\n" +
	     "Commands that have `[params]` formatted like that are optional parameters.\n" +
	     "Commands that have `<params>` formatted like that are required parameters.",
	     CommandAction::help),

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
	                "Everything after the command name is interpreted as part of the request.",
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
	BLUR("blur", "[Radius]",
	     "Blurs an image\n" +
	     "Attach the image to be blurred and optionally specify a radius. If none is specified, 2.0 will be used.",
	     CommandAction::blur),
	MBLUR("mblur", "[Radius] [Sigma]",
	      "Motion blurs an image\n" +
	      "Attach the image to be blurred and optionally specify a radius and sigma. If none is specified, 10 will be used for the radius and 45 will be used for sigma." +
	      "Visit https://imagemagick.org/Usage/blur/#motion-blur for more info",
	      CommandAction::mblur);

	static String prefix = "!";

	final public String name; //Name of the Command
	final private String params; //The params the command takes, only used for !help
	final private String desc; //The desc of the command, only used for !help
	final public ICommandAction action; //Used to execute the action of the command.

	Command (String name, String params, String desc, ICommandAction action) {
		this.name = name;
		this.params = params;
		this.desc = desc;
		this.action = action;
	}

	String getHelpString () {
		return
				name + ":\n" +
				"Syntax `" + prefix + name + " " + params + "`\n" +
				desc;
	}
}
