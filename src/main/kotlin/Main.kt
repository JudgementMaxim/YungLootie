import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.intent.Intent
import org.javacord.api.event.message.MessageCreateEvent

fun main() {
    val api: DiscordApi = DiscordApiBuilder()
        .setToken("MTE4MDI4NjU3NzkxODg3NzgyNw.Gh1tby.lSAXV9nLI5FmR2slES0jAdGqt1gXGxD50b-zu0")
        .addIntents(
            Intent.GUILDS,           // Required for bot to be aware of servers it's in
            Intent.GUILD_MESSAGES,    // Required for bot to receive message events
            Intent.MESSAGE_CONTENT
        )
        .login().join()

    api.addMessageCreateListener { event: MessageCreateEvent ->
        if (event.messageContent.equals("!ping", ignoreCase = true)) {
            event.channel.sendMessage("Pong!")
        }

    }

    println(api.createBotInvite())

    // Your bot logic goes here
}
