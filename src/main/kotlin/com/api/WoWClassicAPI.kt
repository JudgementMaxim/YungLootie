package com.api

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import com.github.kittinunf.fuel.core.extensions.authenticate
import com.github.kittinunf.fuel.core.extensions.authentication
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import java.io.File
import java.io.InputStream

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

        // Print API request details
        println("API Request Details:")
        println("Endpoint: $modifiedEndpoint")
        println("Headers: ${request.headers}")
        println("Authorization: ${request.headers["Authorization"]}")

        return when (result) {
            is Result.Success -> {
                // Print API response details
                println("API Response:")
                println(result.value)
                Response.of(result.value)
            }
            is Result.Failure -> {
                // Print API error details
                println("API Error:")
                println(response.toString())
                Response.error(response.toString())
            }
        }
    }



    // Function to make an API request with client credentials and handle the response
    fun callAPI(region: String, namespace: String, locale: String) {
        val response = getClientCredential(
            "https://oauth.battle.net/token",
            getClientID(),
            getClientSecret(),
            listOf("api1.read", "api1.write")
        ).bind { tokenResponse ->
            callApi(
                "https://eu.api.blizzard.com/data/wow/playable-class/index",
                tokenResponse.tokenType,
                tokenResponse.token,
                namespace
            )
        }

        // Print the response or error
        if (response is Response.Success) {
            println(response.value)
        } else if (response is Response.Failure) {
            println(response.error)
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

                Response.of(
                    TokenResponse(
                        json["access_token"].toString(),
                        json["expires_in"].toString().toInt(),
                        json["token_type"].toString()
                    )
                )
            }
            is Result.Failure -> {
                Response.error("Token request failed")
            }
        }
    }

    // Data class representing token response
    data class TokenResponse(val token: String, val expiresIn: Int, val tokenType: String)

    // Sealed class for handling API responses
    sealed class Response<out L, R> {
        class Success<out L, R>(val value: L) : Response<L, R>()
        class Failure<out L, R>(val error: R) : Response<L, R>()

        // Bind function to handle different response types
        fun <X> bind(success: (L) -> (Response<X, R>)): Response<X, R> {
            return when (this) {
                is Response.Success<L, R> -> success(this.value)
                is Response.Failure<L, R> -> Failure(this.error)
                else -> throw IllegalStateException("Unexpected response type")
            }
        }

        companion object {
            // Function to create a success response
            fun <L, R> of(response: L) = Success<L, R>(response)

            // Function to create an error response
            fun <L, R> error(error: R) = Failure<L, R>(error)
        }
    }
}
