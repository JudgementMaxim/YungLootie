// Define a package for your API-related classes


package com.api

// Import necessary libraries and classes
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import java.io.File
import java.io.InputStream
import org.slf4j.LoggerFactory

// Create a logger instance
private val logger = LoggerFactory.getLogger(WoWClassicAPI::class.java)


// Define a class for interacting with the WoW Classic API
class WoWClassicAPI() {

    // Function to read client ID from a file
    private fun getClientID(): String {
        val txtFile = "src/main/kotlin/com/api/clientID.txt"
        val inputStream: InputStream = File(txtFile).inputStream()
        return inputStream.bufferedReader().use { it.readText() }
    }

    // Function to read client secret from a file
    private fun getClientSecret(): String {
        val txtFile = "src/main/kotlin/com/api/clientSecret.txt"
        val inputStream: InputStream = File(txtFile).inputStream()
        return inputStream.bufferedReader().use { it.readText() }
    }

    // Function to get the authentication token from the token endpoint
    private fun getAuthToken(): Response<TokenResponse, String> {
        val tokenEndpoint = "https://eu.battle.net/oauth/token"  // Update with the correct token endpoint

        val (request, response, result) = tokenEndpoint.httpPost(listOf(
            "grant_type" to "client_credentials"
        ))
            .authentication().basic(getClientID(), getClientSecret())
            .responseString()

        return when (result) {
            is Result.Success -> {
                val parser = Parser()
                val json = parser.parse(StringBuilder(result.value)) as JsonObject

                // Create a success response with the token details
                Response.of(
                    TokenResponse(
                        json["access_token"].toString(),
                        json["expires_in"].toString().toInt(),
                        json["token_type"].toString()
                    )
                )
            }
            is Result.Failure -> {
                // Create an error response if the token request fails
                Response.error("Token request failed")
            }
        }
    }

    // Function to make an API request with a namespace
    fun callApi(apiEndpoint: String, tokenType: String, token: String, namespace: String): Response<String, String> {
        // Modify the endpoint to include the namespace as a query parameter
        val modifiedEndpointQueryParam = "$apiEndpoint?namespace=$namespace"

        // Modify the endpoint to include the namespace as a header
        val modifiedEndpointHeader = apiEndpoint
        val headers = listOf("Battlenet-Namespace" to namespace)

        // Choose one of the modified endpoints based on your preference
        val modifiedEndpoint = modifiedEndpointQueryParam
        // or
        // val modifiedEndpoint = modifiedEndpointHeader.httpGet().header(headers)

        val (request, response, result) = modifiedEndpoint.httpGet()
            .header(Pair("Authorization", "$tokenType $token"))
            .responseString()

        logger.info("API Request Details:")
        logger.info("Method: GET")
        logger.info("Endpoint: $modifiedEndpoint")
        logger.info("Headers: ${request.headers}")
        logger.info("Authorization: ${request.headers["Authorization"]}")
        logger.info("Parameters: ${request.parameters}")

        return when (result) {
            is Result.Success -> {
                // Print API response details
                logger.info("API Response:")
                logger.info(result.value)

                // Create a success response with the API response
                Response.of(result.value)
            }
            is Result.Failure -> {
                // Print API error details
                logger.info("API Error:")
                logger.info(response.toString())

                // Create an error response with the API error details
                Response.error(response.toString())
            }
        }
    }

    // Function to make an API request with client credentials and handle the response
    fun callAPI(region: String, namespace: String, locale: String, endpoint: String): Response<String, String> {
        // Obtain the access token
        val tokenResponse = getAuthToken()
        return when (tokenResponse) {
            is Response.Success -> {
                val accessToken = tokenResponse.value.token
                logger.info("Access Token: $accessToken")

                // Use the obtained access token in the API request
                val response = tokenResponse.bind { token ->
                    callApi(
                        "https://$region.api.blizzard.com/$endpoint",
                        tokenResponse.value.tokenType,
                        tokenResponse.value.token,
                        namespace
                    )
                }

                // Print the response or error
                if (response is Response.Success) {
                    logger.info(response.value)
                } else if (response is Response.Failure) {
                    logger.info(response.error)
                }
                response
            }
            is Response.Failure -> {
                logger.info("Error obtaining access token: ${tokenResponse.error}")
                Response.error(tokenResponse.error)
            }
        }
    }

    // Function to get client credentials
    private fun getClientCredential(
        tokenEndpoint: String,
        clientId: String,
        clientSecret: String,
        scopes: List<String>
    ): Response<TokenResponse, String> {
        val (request, response, result) = tokenEndpoint.httpPost(listOf(
            "grant_type" to "client_credentials",
            "scope" to scopes.joinToString(" ")
        ))
            .authentication().basic(clientId, clientSecret)
            .responseString()

        return when (result) {
            is Result.Success -> {
                val parser = Parser()
                val json = parser.parse(StringBuilder(result.value)) as JsonObject

                // Create a success response with the token details
                Response.of(
                    TokenResponse(
                        json["access_token"].toString(),
                        json["expires_in"].toString().toInt(),
                        json["token_type"].toString()
                    )
                )
            }
            is Result.Failure -> {
                // Create an error response if the token request fails
                Response.error("Token request failed")
            }
        }
    }

    // Data class representing token response
    data class TokenResponse(val token: String, val expiresIn: Int, val tokenType: String)

    // Sealed class for handling API responses
    sealed class Response<out L, R> {
        // Class representing a successful response with a value of type L
        class Success<out L, R>(val value: L) : Response<L, R>()

        // Class representing a failure response with an error of type R
        class Failure<out L, R>(val error: R) : Response<L, R>()

        // Bind function to handle different response types
        fun <X> bind(success: (L) -> (Response<X, R>)): Response<X, R> {
            return when (this) {
                is Response.Success<L, R> -> success(this.value)
                is Response.Failure<L, R> -> Failure(this.error)
                else -> throw IllegalStateException("Unexpected response type")
            }
        }

        // Companion object with utility functions for creating responses
        companion object {
            // Function to create a success response
            fun <L, R> of(response: L) = Success<L, R>(response)

            // Function to create an error response
            fun <L, R> error(error: R) = Failure<L, R>(error)
        }
    }
}
