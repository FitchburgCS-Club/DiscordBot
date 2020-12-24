package com.fsucsc.discordbot;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;

public final class DisConfig {
	static String Token = null; ///Token for the bot

	/*NOTE(Michael): DO NOT SET THIS VARIABLE TO ANYTHING. place the relevant value in the bot's config file.
	 *Refer to the readme.md for how to add a whitelistedUserId value */
	static long WhitelistedUserId = 0; ///If non-zero, contains the user ID of the only user that this bot will listen to.

	final static String OutputDir = ""; // Make sure to include the trailing "/"
	final static ArrayList<String> BlacklistedUsers = new ArrayList<>();

	//TODO: change these into ids
	static TextChannel ErrorChannel = null; ///A channel to use for error reporting. Use this to report to errors when it is unclear what channel the error should be reported in.
	static Role PrivilegedRole;
}
