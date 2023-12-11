// Import necessary classes and packages
// Import custom command classes
import com.api.WoWClassicAPI
import com.commands.MessageCommands
import com.commands.SlashCommands
import org.javacord.api.DiscordApi
import org.javacord.api.DiscordApiBuilder
import org.javacord.api.entity.intent.Intent
import org.javacord.api.event.interaction.SlashCommandCreateEvent
import org.javacord.api.event.message.MessageCreateEvent
import org.javacord.api.interaction.SlashCommand
import java.io.File
import java.io.InputStream

// Create instances of custom command classes
val slashCommands = SlashCommands()
val messageCommands = MessageCommands()
val classicAPI = WoWClassicAPI()

// Enumerates different com.commands the bot can handle
enum class Command(val displayName: String) {
    PING("!ping"), PONG("!PONG");

    val lowerCaseDisplayName: String = displayName.lowercase()
}

// Function to get the bot token from a file
fun getBotToken(): String {
    val txtFile = "botToken.txt"
    val inputStream: InputStream = File(txtFile).inputStream()
    return inputStream.bufferedReader().use { it.readText() }
}

// Function to handle incoming messages
fun handleMessage(event: MessageCreateEvent) {
    // Get the content of the message
    val message = event.messageContent

    // Print the received message
    println(message)

    // Check if the message starts with "!" and the sender is not the bot
    if (message.startsWith("!") && !event.messageAuthor.asUser().map { it.isYourself }.orElse(false)) {
        // Check the type of command
        when (message) {
            Command.PING.lowerCaseDisplayName -> {
                // Handle PING command
                messageCommands.pingMessage(event)
            }
            Command.PONG.lowerCaseDisplayName -> {
                // Handle PONG command
                println("Received PONG command")
            }
            else -> {
                // Handle other cases or invalid com.commands
                println("Invalid command: $message")
            }
        }
    }
}

// Function to handle slash com.commands
fun handleCommands(name: String, event: SlashCommandCreateEvent, commandId: Long) {
    when (name) {
        "ping" -> {
            // Handle ping command
            slashCommands.pingCommand(event, commandId)
        }
        // Add more cases for other com.commands if needed
    }
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

    // Create a global slash command
    val command = SlashCommand.with("ping", "Checks the functionality of this command")
        .createGlobal(api)
        .join()

    // Set up the bot to listen for messages
    setUpBot(api)

    // Add a listener for slash command creation events
    api.addSlashCommandCreateListener { event: SlashCommandCreateEvent ->
        val commandName = event.slashCommandInteraction.commandName
        val id = event.slashCommandInteraction.id
        handleCommands(commandName, event, id)
    }

    classicAPI.callAPI("us", "static-classic-us", "en_US")

    // Print the bot invite link
    println(api.createBotInvite())

    // Additional bot logic goes here
}
