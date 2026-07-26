package io.meyer1994;

import org.apache.camel.Exchange;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.GenericChannelEvent;
import net.dv8tion.jda.api.events.guild.GenericGuildEvent;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveAllEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEmojiEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.events.user.GenericUserEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/** Bridges JDA events into Camel exchanges. */
public class DiscordHandler extends ListenerAdapter {
    private final DiscordConsumer consumer;

    public DiscordHandler(DiscordConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public void onGenericEvent(GenericEvent event) {
        DiscordEvent discordEvent = DiscordEvent.fromEvent(event);
        if (discordEvent == null || this.consumer.endpoint.getEvent() != discordEvent) {
            return;
        }

        Exchange exchange = this.consumer.getEndpoint().createExchange();
        exchange.getMessage().setHeader(DiscordConstants.HEADER_EVENT, discordEvent.name());
        exchange.getMessage().setBody(event);
        this.populateHeaders(exchange, event);

        try {
            this.consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }

    private void populateHeaders(Exchange exchange, GenericEvent event) {
        if (event instanceof MessageReceivedEvent messageEvent) {
            Message message = messageEvent.getMessage();
            setChannelHeaders(exchange, messageEvent);
            setMessageHeaders(exchange, messageEvent);
            setAuthorHeaders(exchange, messageEvent);
            setIsFromHeaders(exchange, messageEvent);
            setIsWebhookHeaders(exchange, messageEvent);
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, messageEvent.getJumpUrl());
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());
        } else if (event instanceof MessageUpdateEvent messageEvent) {
            Message message = messageEvent.getMessage();
            setChannelHeaders(exchange, messageEvent);
            setMessageHeaders(exchange, messageEvent);
            setAuthorHeaders(exchange, messageEvent);
            setIsFromHeaders(exchange, messageEvent);
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, messageEvent.getJumpUrl());
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_TIMESTAMP, message.getTimeCreated());
        } else if (event instanceof MessageDeleteEvent messageEvent) {
            setChannelHeaders(exchange, messageEvent);
            setMessageHeaders(exchange, messageEvent);
            setIsFromHeaders(exchange, messageEvent);
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_URL, messageEvent.getJumpUrl());
        } else if (event instanceof MessageBulkDeleteEvent messageEvent) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, messageEvent.getChannel().getId());
            exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE,
                    messageEvent.getChannel().getType().name());
            setIsFromHeaders(exchange, true, messageEvent.getChannel().getType().isThread(),
                    messageEvent.getGuild().getId());
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_IDS, messageEvent.getMessageIds());
        } else if (event instanceof MessageReactionAddEvent reactionEvent) {
            setReactionHeaders(exchange, reactionEvent);
            setChannelHeaders(exchange, reactionEvent);
            setMessageHeaders(exchange, reactionEvent);
            setIsFromHeaders(exchange, reactionEvent);
            exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_AUTHOR_ID,
                    reactionEvent.getMessageAuthorId());
        } else if (event instanceof MessageReactionRemoveEvent reactionEvent) {
            setReactionHeaders(exchange, reactionEvent);
            setChannelHeaders(exchange, reactionEvent);
            setMessageHeaders(exchange, reactionEvent);
            setIsFromHeaders(exchange, reactionEvent);
        } else if (event instanceof MessageReactionRemoveAllEvent reactionEvent) {
            setChannelHeaders(exchange, reactionEvent);
            setMessageHeaders(exchange, reactionEvent);
            setIsFromHeaders(exchange, reactionEvent);
        } else if (event instanceof MessageReactionRemoveEmojiEvent reactionEvent) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_EMOJI, reactionEvent.getEmoji().getName());
            setChannelHeaders(exchange, reactionEvent);
            setMessageHeaders(exchange, reactionEvent);
            setIsFromHeaders(exchange, reactionEvent);
        } else if (event instanceof GenericMessageEvent messageEvent) {
            setChannelHeaders(exchange, messageEvent);
            setMessageHeaders(exchange, messageEvent);
            setIsFromHeaders(exchange, messageEvent);
        } else if (event instanceof GenericChannelEvent channelEvent) {
            setChannelHeaders(exchange, channelEvent);
            setIsFromHeaders(exchange, channelEvent);
        } else if (event instanceof GenericInteractionCreateEvent interactionEvent) {
            if (interactionEvent.getChannel() != null) {
                exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID,
                        interactionEvent.getChannel().getId());
            }
            if (interactionEvent.getGuild() != null) {
                exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID,
                        interactionEvent.getGuild().getId());
            }
            if (interactionEvent.getUser() != null) {
                exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID,
                        interactionEvent.getUser().getId());
            }
        } else if (event instanceof GenericGuildEvent guildEvent) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, guildEvent.getGuild().getId());
        } else if (event instanceof GenericUserEvent userEvent) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID, userEvent.getUser().getId());
        }
    }

    private void setAuthorHeaders(Exchange exchange, MessageReceivedEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID, event.getAuthor().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_IS_BOT, event.getAuthor().isBot());
    }

    private void setAuthorHeaders(Exchange exchange, MessageUpdateEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_ID, event.getAuthor().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_AUTHOR_IS_BOT, event.getAuthor().isBot());
    }

    private void setMessageHeaders(Exchange exchange, GenericMessageEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_MESSAGE_ID, event.getMessageId());
    }

    private void setReactionHeaders(Exchange exchange, GenericMessageReactionEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_REACTION_USER_ID, event.getUserId());
    }

    private void setChannelHeaders(Exchange exchange, GenericMessageEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
    }

    private void setChannelHeaders(Exchange exchange, GenericChannelEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_ID, event.getChannel().getId());
        exchange.getMessage().setHeader(DiscordConstants.HEADER_CHANNEL_TYPE, event.getChannelType().name());
    }

    private void setIsFromHeaders(Exchange exchange, GenericMessageEvent event) {
        setIsFromHeaders(exchange, event.isFromGuild(), event.isFromThread(),
                event.isFromGuild() ? event.getGuild().getId() : null);
    }

    private void setIsFromHeaders(Exchange exchange, GenericChannelEvent event) {
        setIsFromHeaders(exchange, event.isFromGuild(), false,
                event.isFromGuild() ? event.getGuild().getId() : null);
    }

    private void setIsFromHeaders(Exchange exchange, boolean fromGuild, boolean fromThread, String guildId) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_GUILD, fromGuild);
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_FROM_THREAD, fromThread);
        if (fromGuild && guildId != null) {
            exchange.getMessage().setHeader(DiscordConstants.HEADER_GUILD_ID, guildId);
        }
    }

    private void setIsWebhookHeaders(Exchange exchange, MessageReceivedEvent event) {
        exchange.getMessage().setHeader(DiscordConstants.HEADER_IS_WEBHOOK, event.isWebhookMessage());
    }
}
