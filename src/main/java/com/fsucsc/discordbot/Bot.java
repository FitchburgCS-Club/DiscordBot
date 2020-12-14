package com.fsucsc.discordbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Bot {
	static ScheduledExecutorService TaskScheduler;
	
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
	 * @param ex the exception
	 * @param channel the channel to send to
	 */
	static void ReportStackTrace (Exception ex, MessageChannel channel) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		//TODO(Michael): Should we make a bunch of random error messages?
		//na... only I would find that funny.
		Bot.SendMessage(channel, "An unexpected exception occurred! You should show this to a programmer-- oh wait...\n" + sw.toString());
	}

	public static void main (String[] args) {
		TaskScheduler = Executors.newScheduledThreadPool(1);
		
		try (Scanner scan = new Scanner(new File("config"))) {
			DisConfig.token = scan.nextLine();
			//TODO(Michael): Load tasks that were not completed before we shutdown last.
			//Note(Michael): having a line for the whitelistedUserId is optional.
			if (scan.hasNextLine()) {
				DisConfig.whitelistedUserId = Long.parseLong(scan.nextLine());
			}
			else {
				DisConfig.whitelistedUserId = 0;
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
			JDABuilder.createDefault(DisConfig.token)
			          .addEventListeners(new CommandListener())
			          .build();
		}
		catch (LoginException | IllegalArgumentException ex) {
			System.out.println("Failed to Log in");
			System.exit(-2);
		}
	}
}
