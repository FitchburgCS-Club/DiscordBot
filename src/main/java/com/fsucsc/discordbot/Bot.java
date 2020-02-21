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
		String token = null;
		try {
			Scanner scan = new Scanner(new File("token"));
			token = scan.nextLine();
		}
		catch (FileNotFoundException | NoSuchElementException e) {
			System.out.println("Error opening and reading token file.");
			System.out.println("Are you sure it exists and contains the token for the discord bot?");
			//NOTE(Michael): when working on the project, the 'token' file should be placed at the root of the repo
			System.exit(-1);
		}

		try {
			JDA jda = new JDABuilder(AccountType.BOT).setToken(token).addEventListeners(new CommandListener()).build();
		}
		catch (LoginException | IllegalArgumentException e) {
			System.out.println("Failed to Log in");
			System.exit(-2);
		}
	}
}
