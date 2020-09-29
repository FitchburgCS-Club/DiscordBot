package com.fsucsc.discordbot;

import java.util.ArrayList;
import java.util.List;

public final class DisConfig {
	static String token = null; ///Token for the bot

	/*NOTE(Michael): DO NOT SET THIS VARIABLE TO ANYTHING. place the relevant value in the bot's config file.
	 *Refer to the readme.md for how to add a whitelistedUserId value */
	static long whitelistedUserId = 0; ///If non-zero, contains the user ID of the only user that this bot will listen to.

	final static String outputDir = ""; // Make sure to include the trailing "/"
	final static List<String> blackListedUsers = new ArrayList<>();
}
