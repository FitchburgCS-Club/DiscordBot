package com.fsucsc.discordbot;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

import java.awt.*;
import java.io.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Arrays;

class MeetingNotif implements Runnable {
	String message;
	String sendChannelId; // string rep of a long.
	long notifyTime; // in miliseconds since epoch.
	String subcribedRoleId; // id of the role we will mention. 0 indicates that we will mention everyone.

	MeetingNotif (String newMessage, String newSendChannelId, long newNotifTime, String newSubcribedRole) {
		TextChannel sendChannel = Bot.Jda.getTextChannelById(newSendChannelId);
		if (sendChannel != null) {
			message = newMessage;
			sendChannelId = newSendChannelId;
			notifyTime = newNotifTime;
			subcribedRoleId = newSubcribedRole;

			try (FileWriter fw = new FileWriter(DisConfig.OutputDir + "meetings.txt", true)) {
				fw.write(toString() + "\n");
			}
			catch (IOException ex) {
				Bot.SendMessage(Bot.Jda.getTextChannelById(newSendChannelId), "Failed to open '" + DisConfig.OutputDir + "meetings.txt'.");
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(Bot.Jda.getTextChannelById(newSendChannelId), ex);
			}
		}
		else {
			throw new NullPointerException("JDA could not get the text channel from the channel id '" + newSendChannelId + "' This likely means that the channel does not exist...");
		}
	}

	public static void tryToMakeFromString (String data) {
		int[] pipePos = new int[4];
		pipePos[0] = data.indexOf("|");
		pipePos[1] = data.indexOf("|", pipePos[0] + 1);
		pipePos[2] = data.indexOf("|", pipePos[1] + 1);
		pipePos[3] = data.indexOf("|", pipePos[2] + 1);

		long notifyTime = Long.parseLong(data.substring(pipePos[0] + 1, pipePos[1]));
		String message = data.substring(pipePos[1] + 1, pipePos[2]);
		String sendChannelId = data.substring(pipePos[2] + 1, pipePos[3]);
		String subscribedRole = data.substring(pipePos[3] + 1);

		if (!subscribedRole.equals("0")) {
			try {
				Role temp = Bot.Jda.getTextChannelById(sendChannelId).getGuild().getRolesByName(subscribedRole, false).get(0);
				if (!temp.getName().equals(subscribedRole)) {
					Bot.SendMessage(DisConfig.ErrorChannel, "The old announcement **" + message + "** in <#" + sendChannelId + ">  at **" + new Date(notifyTime).toString() + "** has been discarded as the role it was directed at no longer exists.");
					return;
				}
			}
			catch (IndexOutOfBoundsException ex) {
				Bot.SendMessage(DisConfig.ErrorChannel, "The old announcement **" + message + "** in <#" + sendChannelId + ">  at **" + new Date(notifyTime).toString() + "** has been discarded as the role it was directed at no longer exists.");
				return;
			}
		}

		long meetingDelta = notifyTime - new Date().getTime();
		if (meetingDelta > 0) {
			Bot.TaskScheduler.schedule(new MeetingNotif(message, sendChannelId, notifyTime, subscribedRole), meetingDelta, TimeUnit.MILLISECONDS);
			Bot.SendMessage(DisConfig.ErrorChannel, "The old announcement **" + message + "** directed to " + subscribedRole + "in <#" + sendChannelId + ">  at **" + new Date(notifyTime).toString() + "** has been reloaded");
			return;
		}
		else {
			Bot.SendMessage(DisConfig.ErrorChannel, "The old announcement **" + message + "** directed to " + subscribedRole + " in <#" + sendChannelId + "> at **" + new Date(notifyTime).toString() + "** has been discarded as it's time has passed.");
			return;
		}
	}

	@Override
	public String toString () {
		return hashCode() + "|" + notifyTime + "|" + message + "|" + sendChannelId + "|" + subcribedRoleId;
	}

	@Override
	public void run () {
		TextChannel sendChannel = Bot.Jda.getTextChannelById(sendChannelId);
		if (sendChannel != null) {

			if (subcribedRoleId.equals("0")) {
				Bot.SendMessage(sendChannel, "@ everyone " + message);
			}
			else {
				Bot.SendMessage(sendChannel, "<@&" + subcribedRoleId + "> " + message);
			}

			String[] allLinesArr = new String[0];

			try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.OutputDir + "meetings.txt"))) {
				Stream<String> allLines = br.lines();
				allLines = allLines.filter((String line)->!line.startsWith(toString())); // Using startsWith just in case line contains a '\n' at the end
				allLinesArr = allLines.toArray(String[]::new);
			}
			catch (IOException ex) {
				Bot.SendMessage(DisConfig.ErrorChannel, "There was an error reading/writing to '" + DisConfig.OutputDir + "meetings.txt'.");
				Bot.SendMessage(DisConfig.ErrorChannel, "ex.toString(): " + ex.toString());
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(DisConfig.ErrorChannel, ex);
			}

			try (FileWriter fw = new FileWriter(DisConfig.OutputDir + "meetings.txt")) {
				for (Object line : allLinesArr) {
					fw.write(line + "\n");
				}
			}
			catch (IOException ex) {
				Bot.SendMessage(DisConfig.ErrorChannel, "There was an error reading/writing to '" + DisConfig.OutputDir + "meetings.txt'.");
				Bot.ReportStackTrace(DisConfig.ErrorChannel, ex);
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(DisConfig.ErrorChannel, ex);
			}
		}
		else {
			NullPointerException ex = new NullPointerException("JDA could not get the text channel from the channel id '" + sendChannelId + "' This likely means that the channel does not exist anymore...");
			//reporting to ErrorChannel because we don't know what channel we should output to...
			//reporting before thowing because when the exeception is thrown, this thread will scilently die.
			Bot.ReportStackTrace(DisConfig.ErrorChannel, ex);
			throw ex;
		}

	}
}

public enum Command {
	PING("ping", "", false,
	     "The father of all commands.\n" +
	     "Pings the bot, causing it to pong.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			Bot.SendMessage(event, "Pong!");
		}
	},

	HELP("help", "[CommandName]", false,
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

	BLACKLIST("blacklist", "[add|remove] [MentionedUser]", false,
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
					for (String user : DisConfig.BlacklistedUsers) {
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
							if (DisConfig.BlacklistedUsers.contains(usr.getId())) {
								Bot.SendMessage(event.getChannel(), usr.getName() + " is already blacklisted!");
							}
							else {
								DisConfig.BlacklistedUsers.add(usr.getId());
								Bot.SendMessage(event.getChannel(), usr.getName() + " has been added to blacklist.");
							}
						}
						else if (args.startsWith("remove")) {
							if (!DisConfig.BlacklistedUsers.contains(usr.getId())) {
								Bot.SendMessage(event.getChannel(), usr.getName() + " is not blacklisted!");
							}
							else {
								DisConfig.BlacklistedUsers.remove(usr.getId());
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
				Bot.ReportStackTrace(event.getChannel(), ex);
			}
		}
	},

	FEATURE_REQUEST("featureRequest", "<Request>", false,
	                "Request a feature to be added to the bot.\n" +
	                "Everything after the command name is interpreted as part of the request.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			try (FileWriter fw = new FileWriter(DisConfig.OutputDir + "FeatureRequests.txt", true)) {
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
				Bot.ReportStackTrace(event.getChannel(), ex);
			}
		}
	},

	LIST_REQUESTS("listRequests", "", false,
	              "List current requests for the bot.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.OutputDir + "FeatureRequests.txt"))) {
				Stream<String> lines = br.lines();
				StringBuilder reply = new StringBuilder(2000);
				List<String> requestlist = lines.collect(Collectors.toList());
				for (int i = 1; i < requestlist.size() + 1; i++) {
					String[] parts = requestlist.get(i - 1).split("\\|");
					//NOTE(Michael): parts[0] == Content, parts[1] == Author
					parts[1] = event.getGuild()
					                .getMemberById(parts[1])
					                .getEffectiveName();
					// @Robustness This function will will throw a null pointer exception if the user who requested something has left the server.
					// We ought to create a error handler for this.
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
				Bot.ReportStackTrace(event.getChannel(), ex);
			}
		}
	},

	REMOVE_REQUEST("removeRequest", "<Index>", false,
	               "Removes a request from the feature request list\n" +
	               "Use `!listRequests` to find the index of a feature request") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			int requestNum = Integer.parseInt(args) - 1;
			String oldRequest;
			try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.OutputDir + "FeatureRequests.txt"))) {
				Stream<String> lines = br.lines();
				List<String> requestlist = lines.collect(Collectors.toList());
				try {
					oldRequest = requestlist.get(requestNum).split("\\|")[0];
					requestlist.remove(requestNum);
					BufferedWriter bw = new BufferedWriter(new FileWriter(DisConfig.OutputDir + "FeatureRequests.txt"));
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
				Bot.ReportStackTrace(event.getChannel(), ex);
			}
		}
	},

	MACKAY_STANDARD("mackayStandard", "", false,
	                "Let's you know what happens when you don't follow Mackay Standards.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			EmbedBuilder builder = new EmbedBuilder();
			builder.setColor(Color.RED)
			       .setImage("attachment://mackaystandard.jpeg")
			       .setTitle("Warning")
			       .setDescription("If you don't follow the Mackay standard,, this could be you!");
			try {
				InputStream img = new FileInputStream("./img/mackaystandard.jpg");
				event.getChannel()
				     .sendFile(img, "mackaystandard.jpg") //TODO(Michael): I think there's a way to include images in embeds? might be a cool edit.
				     .embed(builder.build())
				     .queue();
			}
			catch (FileNotFoundException ex) {
				Bot.SendMessage(event, "Error:\nImage not found on server.");
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(event.getChannel(), ex);
			}
		}
	},

	//Note(Michael): This command is DANGEROUS AND ARMED. if you are testing this, please change the '@everyone' in the MeetingNotif class to something else.
	SCHEDULE_ANNOUNCEMENT("scheduleAnnouncement", "<yyyy-MM-dd HH:mm> [Channel] | <Text> [| <Role Name>]", true,
	                      "Schedules an announcement at the spefied time.\n" +
	                      "`<yyyy-MM-dd HH:mm>` refers to the date and time at which the announcement will be shown.\n" +
	                      "y stands for year, M stands for month, d stands for day, H stands for hour in 24 hr format where '0' is midnight\n" +
	                      "m stands for minute.\n" +
	                      "[Channel] is a channel mention that spefies what channel the announcement will appear in.\n" +
	                      "`<text>` is the text that will show when the reminder sends.\n" +
	                      "`[| <Role Name>]` is the literal character '|' followed by the name of a role. DO NOT include a '@' symbol\n" +
	                      "This command is only applicible if used from a Text Channel in a Guild.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			MessageChannel sendChannel = null;
			if (!event.getMessage().getMentionedChannels().isEmpty()) {
				sendChannel = event.getMessage().getMentionedChannels().get(0);
				args = args.substring(0, args.indexOf("<#")) + args.substring(args.indexOf(">") + 1);
			}
			else {
				sendChannel = event.getChannel();
			}

			if (sendChannel.getType() == ChannelType.TEXT) { //This command is only applicible if in a guild text channel.
				int[] pipePos = new int[2];
				pipePos[0] = args.indexOf("|");
				pipePos[1] = args.indexOf("|", pipePos[0] + 1);

				String strDate = args.substring(0, pipePos[0]);
				String mentionedRoleId = "0";
				Role mentionedRole = null;
				try {
					if (pipePos[1] != -1) {
						mentionedRole = event.getGuild()
						                     .getRolesByName(args.substring(pipePos[1] + 2), false)
						                     .get(0);
						mentionedRoleId = mentionedRole.getId();
					}
				}
				catch (IndexOutOfBoundsException ex) {
					Bot.SendMessage(event, "The supplied role does not exist!");
					return;
				}

				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				try {
					long notifyTime = df.parse(strDate).getTime();
					long curTime = new Date().getTime();
					long notifyDelta = notifyTime - curTime;

					if (notifyDelta <= 0) {
						Bot.SendMessage(event, "The supplied date is in the past!");
						return;
					}
					String message = null;
					if (pipePos[1] == -1)
						message = args.substring(pipePos[0] + 2); //Note(Michael): to consume the ' ' after the pipe.
					else
						message = args.substring(pipePos[0] + 2, pipePos[1] - 1); //Note(Michael): to consume the ' ' after and before pipes.

					Bot.TaskScheduler.schedule(new MeetingNotif(message, sendChannel.getId(), notifyTime, mentionedRoleId), notifyDelta, TimeUnit.MILLISECONDS);
					if (mentionedRoleId.equals("0"))
						Bot.SendMessage(event, "Everyone will be notified at **" + strDate.trim() + "** in <#" + sendChannel.getId() + "> about **" + message + "**");
					else
						Bot.SendMessage(event, mentionedRole.getName() + " will be notified at **" + strDate.trim() + "** in <#" + sendChannel.getId() + "> about **" + message + "**");

				}
				catch (ParseException ex) {
					Bot.SendMessage(event, "The supplied date was invalid!\nSubmit your date in 'yyyy-MM-dd HH:mm' format");
				}
				catch (IndexOutOfBoundsException ex) {
					Bot.SendMessage(event, "The `<Text>` parameter was invalid");
				}
				catch (Exception ex) {
					Bot.ReportStackTrace(event.getChannel(), ex);
				}

			}
			else {
				Bot.SendMessage(event, "This command is only applicible from a guild.");
			}
		}
	},
	BLUR("blur", "[blurAmount]", false, "Blurs an attached image. You can optionally specify an amount for blurring. The command defaults to 2.0. The max blur is 150.") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			String tmpDir = System.getProperty("java.io.tmpdir");
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
						double blurAmnt;
						try {
							blurAmnt = Double.parseDouble(finalArgs);
                            if(blurAmnt > 150) {
                                blurAmnt = 150;
                            }
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
						try {
							InputStream img = new FileInputStream(tmpDir + "/blur_" + attachmentName);
							event.getChannel().sendFile(img, attachmentName).queue();
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
	},
	MBLUR("mblur", "[blurAmount] [blurAngle]", false, "Applies motion blur to an image. Defaults are 10.0 and 45.0. The max blur is 150 and the max angle is 360") {
		@Override
		public void execute (MessageReceivedEvent event, String args) {
			String tmpDir = System.getProperty("java.io.tmpdir");
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
						double blurAmnt;
						double blurAngle;
						try {
							blurAmnt = Double.parseDouble(finalArgs[0]);
							blurAngle = Double.parseDouble(finalArgs[1]);
                            if(blurAmnt > 150) {
                                blurAmnt = 150;
                            }
                            if(blurAngle > 360) {
                                blurAngle = 360;
                            }
						}
						catch (Exception ex) {
							blurAmnt = 10.0;
							blurAngle = 45.0;
						}
						ConvertCmd cmd = new ConvertCmd();
						IMOperation op = new IMOperation();
						op.addImage(tmpDir + "/" + attachmentName);
						op.motionBlur(0.0, blurAmnt, blurAngle);
						op.addImage(tmpDir + "/blur_" + attachmentName);
						cmd.run(op);
						File f = new File(tmpDir + "/blur_" + attachmentName);
						try {
							InputStream img = new FileInputStream(tmpDir + "/blur_" + attachmentName);
							event.getChannel().sendFile(img, attachmentName).queue();
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
	},
	JUSTWORKS("justworks", "[type (bill|linus|steve|todd)] <caption>", false, "Lets you know what \"just works\" courtesy of either Bill Gates, Linus Torvalds, Steve Jobs or Todd Howard"){
		@Override
		public void execute (MessageReceivedEvent event, String args) {
            // TODO(Zack)
            // Split the text into lines if it becomes to wide
            // Make a better way of centering text, the current way is garbagio
			String tmpDir = System.getProperty("java.io.tmpdir");
            int[] coords = {0,0};
            String font;
            int fontSize;
            String[] argsa = args.split(" ");
            String color;
            String name = argsa[0].toLowerCase();
            String caption = String.join(" ", Arrays.copyOfRange(argsa, 1, argsa.length));
            float avgLtrWidth;
            float avgLtrWidthMultiple = 0.4126f; // The average width of a letter in 11pt Arial / 11
            int leftShift; // Amount to shift text to the left so it's centered

            switch(name) {
                case "bill":
                    coords[0] = 360;
                    coords[1] = 512;
                    color = "white";
                    fontSize = 52;
                    avgLtrWidth = fontSize * avgLtrWidthMultiple;
                    leftShift = Math.round((caption.length()/2) * avgLtrWidth);
                    font = "Arial";
                    break;
                case "linus":
                    coords[0] = 275;
                    coords[1] = 375;
                    color = "white";
                    fontSize = 51;
                    avgLtrWidth = fontSize * avgLtrWidthMultiple;
                    leftShift = Math.round((caption.length()/2) * avgLtrWidth);
                    font = "Arial";
                    break;
                case "steve":
                    coords[0] = 595;
                    coords[1] = 420;
                    color = "white";
                    fontSize = 45;
                    avgLtrWidth = fontSize * avgLtrWidthMultiple;
                    leftShift = Math.round((caption.length()/2) * avgLtrWidth);
                    font = "Arial";
                    break;
                case "todd":
                    coords[0] = 925;
                    coords[1] = 320;
                    color = "black";
                    fontSize = 40;
                    avgLtrWidth = fontSize * avgLtrWidthMultiple;
                    leftShift = Math.round((caption.length()/2) * avgLtrWidth);
                    font = "Arial";
                    break;
                default:
                    Bot.SendMessage(event.getChannel(), "Error:\nInvalid type!");
                    return;
            }
            try {
                ConvertCmd cmd = new ConvertCmd();
                IMOperation op = new IMOperation();

                op.addImage("img/"+name+"_just_works.jpg");
                op.fill(color);
                op.pointsize(fontSize);
                op.annotate(0,0,coords[0] - leftShift,coords[1],caption);
                op.addImage(tmpDir + "/" + name + "_just_works.jpg");

                cmd.run(op);
                File f = new File(tmpDir + "/" + name + "_just_works.jpg");
                try {
                    InputStream img = new FileInputStream(tmpDir + "/" + name + "_just_works.jpg");
                    event.getChannel().sendFile(img, "It Just Works.jpg").queue();
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
            } catch (FileNotFoundException ex) {
				Bot.SendMessage(event, "Error:\nImage not found on server.");
            } catch (Exception ex) {
                Bot.SendMessage(event.getChannel(), "Failed to process image!");
                Bot.ReportStackTrace(event.getChannel(), ex);
            }
		}
	};

	static String prefix = "!"; //The prefix for all commands.

	final public String name; ///Name of the Command
	final private String params; ///The params the command takes, only used for !help
	final public boolean requiresPrivilegedRole; ///If this command requires the user to have elevated permissions.
	final private String desc; ///The desc of the command, only used for !help


	Command (String name, String params, boolean reqPrivRole, String desc) {
		this.name = name;
		this.params = params;
		this.desc = desc;
		this.requiresPrivilegedRole = reqPrivRole;
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
