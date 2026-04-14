package uz.alphazet.data.services

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Tiny helper shared by the service-interface tests. Builds a Retrofit client
 * pointed at the given [MockWebServer] and creates the requested service.
 * Also provides [mockOk] / [mockError] for enqueuing JSON-bodied responses.
 */
internal inline fun <reified T> retrofitFor(server: MockWebServer): T =
    Retrofit.Builder()
        .baseUrl(server.url("/"))
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(T::class.java)

internal fun mockOk(body: String): MockResponse =
    MockResponse().setResponseCode(200).setBody(body)

internal fun mockError(code: Int, body: String = ""): MockResponse =
    MockResponse().setResponseCode(code).setBody(body)