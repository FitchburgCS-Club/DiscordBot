package com.fsucsc.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import javax.security.auth.login.LoginException;
import java.io.*;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bot {
	static ScheduledExecutorService TaskScheduler;
	static JDA Jda;

	/**
	 * Wrapper function to simplify sending messages to a channel.
	 * This function will automatically split up messages that are greater than
	 * Discord's message character limit (2000 chars).
	 *
	 * @param channel the channel to send the message to
	 * @param contents string containing the message contents
	 */
	static void SendMessage (MessageChannel channel, String contents) {
		System.out.println("Sending Message: " + contents);
		try {
			do
			{
				channel.sendMessage(contents.substring(0, 2000)).queue();
				contents = contents.substring(2000);
			} while (contents.length() > 2000);
		}
		catch (IndexOutOfBoundsException e) {
			channel.sendMessage(contents).queue();
		}
	}

	/**
	 * Convenience overload for SendMessage
	 * Will send contents to whatever channel event came from.
	 */
	static void SendMessage (GenericMessageEvent event, String contents) {
		SendMessage(event.getChannel(), contents);
	}

	/**
	 * Function that reports exceptions to a discord channel
	 *
	 * @param channel the channel to send to
	 * @param ex the exception
	 */
	static void ReportStackTrace (MessageChannel channel, Exception ex) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		//TODO(Michael): Should we make a bunch of random error messages?
		//na... only I would find that funny...
		Bot.SendMessage(channel, "An unexpected exception occurred! You should show this to a programmer-- oh wait...\n" + sw.toString());
	}

	public static void main (String[] args) {
		TaskScheduler = Executors.newScheduledThreadPool(1);
		String errorChannelId = null;
		String privlagedRoleId = null;

		try (Scanner scan = new Scanner(new File("config"))) {
			DisConfig.Token = scan.nextLine();
			errorChannelId = scan.nextLine();
			privlagedRoleId = scan.nextLine();

			//TODO(Michael): Load tasks that were not completed before we shutdown last.
			if (scan.hasNextLine()) { //Note(Michael): having a line for the whitelistedUserId is optional.
				DisConfig.WhitelistedUserId = Long.parseLong(scan.nextLine());
			}
			else {
				DisConfig.WhitelistedUserId = 0;
			}
		}
		catch (FileNotFoundException | NoSuchElementException ex) {
			System.out.println("Error opening and reading config file.");
			System.out.println("Are you sure it exists and contains the token for the discord bot?");
			//NOTE(Michael): when working on the project, the 'config' file should be placed at the root of the repo
			System.exit(-1);
		}
		catch (Exception ex) {
			System.out.println("Abnormal error occurred");
			ex.printStackTrace();
			System.exit(-1);
		}

		try {
			Jda = JDABuilder.createDefault(DisConfig.Token)
			                .addEventListeners(new CommandListener())
			                .build()
			                .awaitReady();
		}
		catch (LoginException | IllegalArgumentException ex) {
			System.out.println("Failed to Log in");
			System.exit(-2);
		}
		catch (InterruptedException ignored) {}

		if (errorChannelId != null) {
			DisConfig.ErrorChannel = Jda.getTextChannelById(errorChannelId);
		}

		if (privlagedRoleId != null) {
			DisConfig.PrivilegedRole = Jda.getRoleById(privlagedRoleId);
		}

		String[] oldMeetings = new String[0];
		try (BufferedReader br = new BufferedReader(new FileReader(DisConfig.OutputDir + "meetings.txt"))) {
			oldMeetings = br.lines().toArray(String[]::new);
			br.close();
			new FileWriter(DisConfig.OutputDir + "meetings.txt").close(); // clears the file.
		}
		catch (FileNotFoundException ignored) {}
		catch (IOException ex) {
			Bot.SendMessage(DisConfig.ErrorChannel, "There was a reading 'meetings.txt' on startup.");
			Bot.ReportStackTrace(DisConfig.ErrorChannel, ex);
		}

		for (String s : oldMeetings) {
			try {
				MeetingNotif.tryToMakeFromString(s);
			}
			catch (Exception ex) {
				Bot.ReportStackTrace(DisConfig.ErrorChannel, ex);
			}
		}

	}
}
