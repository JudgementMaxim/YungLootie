package commands

import org.javacord.api.event.interaction.SlashCommandCreateEvent

class SlashCommands {
    fun pingCommand(event: SlashCommandCreateEvent, commandId : Long){
        event.getInteraction()
            .createImmediateResponder()
            .setContent("Pong")
            .respond();
    }
}