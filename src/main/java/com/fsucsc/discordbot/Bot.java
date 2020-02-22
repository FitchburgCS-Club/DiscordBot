package com.fsucsc.discordbot;

import net.dv8tion.jda.api.AccountType;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class Bot {
	public static void main (String[] args) {
		try {
			Scanner scan = new Scanner(new File("config"));
			DisConfig.token = scan.nextLine();
			if (scan.hasNextLine()) {
				DisConfig.whitelistedUser = scan.nextLine();
			}
		}
		catch (FileNotFoundException | NoSuchElementException e) {
			System.out.println("Error opening and reading config file.");
			System.out.println("Are you sure it exists and contains the token for the discord bot?");
			//NOTE(Michael): when working on the project, the 'config' file should be placed at the root of the repo
			System.exit(-1);
		}

		try {
			 new JDABuilder(AccountType.BOT).setToken(DisConfig.token).addEventListeners(new CommandListener()).build();
		}
		catch (LoginException | IllegalArgumentException e) {
			System.out.println("Failed to Log in");
			System.exit(-2);
		}
	}
}
