package commands

import org.javacord.api.event.message.MessageCreateEvent

class MessageCommands {
    fun pingMessage(event: MessageCreateEvent){
        event.getChannel().sendMessage("Pong")
    }

}