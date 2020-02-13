package com.fsucsc.discordbot;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class Bot {
	private static final Map<String, Command> commands = new HashMap<>();

	static {
		commands.put("ping", event -> event.getMessage().getChannel()
				.flatMap(channel -> channel.createMessage("Pong!"))
				.then());
	}

	public static void main(String[] args) {
		// TODO: Add token, possibly read in from file for security reasons
		DiscordClientBuilder builder = new DiscordClientBuilder("");
		DiscordClient client = builder.build();

		client.getEventDispatcher().on(MessageCreateEvent.class)
				.flatMap(event -> Mono.justOrEmpty(event.getMessage())
                        // Makes sure it's not responding to itself
                        .filter(message -> message.getAuthor().map(user -> !user.isBot()).orElse(false))
						.flatMap(content -> Flux.fromIterable(commands.entrySet())
								// We will be using ! as our "prefix" to any command in the system.
								.filter(entry -> content.getContent().orElse("").startsWith('!' + entry.getKey()))
								.flatMap(entry -> entry.getValue().execute(event))
								.next()))
				.subscribe();

		client.login().block();
	}
}
