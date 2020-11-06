package com.fsucsc.discordbot;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Bot {
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
	 * Function that reports exceptions to a discord channel
	 *
	 * @param ex the exception
	 * @param channel the channel to send to
	 */
	static void ReportStackTrace (Exception ex, MessageChannel channel) {
		StringWriter sw = new StringWriter();
		ex.printStackTrace(new PrintWriter(sw));
		Bot.SendMessage(channel, "An unexpected exception occurred! You should show this to a programmer-- oh wait...\n" + sw.toString());
	}

	public static void main (String[] args) {
		try (Scanner scan = new Scanner(new File("config"))) {
			DisConfig.token = scan.nextLine();
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
