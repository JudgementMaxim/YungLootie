import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.intent.Intent
import org.javacord.api.event.message.MessageCreateEvent
import java.io.File
import java.io.InputStream

fun getBotToken() : String{
    val txtFile = "C:/Users/skale/IdeaProjects/JungerLooterBot/botToken.txt"
    val inputStream: InputStream = File(txtFile).inputStream()
    val inputString = inputStream.bufferedReader().use {it.readText()}

    return inputString
}

fun main() {


    val api: DiscordApi = DiscordApiBuilder()
        .setToken(getBotToken())
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
