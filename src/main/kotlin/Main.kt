import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.intent.Intent
import org.javacord.api.event.interaction.SlashCommandCreateEvent
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommand
import java.io.File
import java.io.InputStream


// Enumerates different commands the bot can handle
enum class Command(val displayName: String) {
    PING("!ping"), PONG("!PONG");

    val lowerCaseDisplayName: String = displayName.lowercase()
}


// Function to get the bot token from a file
fun getBotToken(): String {
    val txtFile = "C:/Users/Schueler/IdeaProjects/JungerLooter/botToken.txt"
    val inputStream: InputStream = File(txtFile).inputStream()
    return inputStream.bufferedReader().use { it.readText() }
}



// Function to handle incoming messages
fun handleMessage(event: MessageCreateEvent) {
    val message = event.messageContent

    println(message)

    // Check if the message starts with "!"
    if (message.startsWith("!")) {

        // Check if the message sender is not the bot itself
        if (!event.messageAuthor.asUser().map { it.isYourself }.orElse(false)) {
            when (message) {
                Command.PING.lowerCaseDisplayName -> {
                    // Handle PING command
                    pingCommand(event)
                }
                Command.PONG.lowerCaseDisplayName -> {
                    // Handle PONG command
                    println("Received PONG command")
                }
                else -> {
                    // Handle other cases or invalid commands
                    println("Invalid command: $message")
                }
            }
        }
    }
}



fun pingCommand(event: MessageCreateEvent){
    event.getChannel().sendMessage("Pong")
}

// Function to set up and start listening for messages
fun setUpBot(api: DiscordApi) {
    // Add a listener for message creation events
    api.addMessageCreateListener { event: MessageCreateEvent ->
        handleMessage(event)
    }
}

// Main function where the bot is initialized and started
fun main() {
    // Create a DiscordApi instance with the bot token and specified intents
    val api: DiscordApi = DiscordApiBuilder()
        .setToken(getBotToken())
        .addIntents(
            Intent.GUILDS,           // Required for bot to be aware of servers it's in
            Intent.GUILD_MESSAGES,    // Required for bot to receive message events
            Intent.MESSAGE_CONTENT
        )
        .login().join()
    val command = SlashCommand.with("ping", "Checks the functionality of this command")
        .createGlobal(api)
        .join()



    // Set up the bot to listen for messages
    setUpBot(api)

    api.addSlashCommandCreateListener { event: SlashCommandCreateEvent ->
        val interaction = event.slashCommandInteraction
        val commandName = interaction.commandName


    }



    // Print the bot invite link
    println(api.createBotInvite())

    // Additional bot logic goes here
}
