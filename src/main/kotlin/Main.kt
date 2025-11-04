package org.gic

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.gic.model.ApiResponse
import org.gic.model.TestData
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

val mapper = jacksonObjectMapper()
const val testDataFileName : String = "test_data_ariatransaction.json"
val client: OkHttpClient = getUnsafeOkHttpClient()
val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
val GREEN = "\u001B[32m"
val RED = "\u001B[31m"
val RESET = "\u001B[0m"

fun main() {
    val testDataList : List<TestData> = readFile()
    println("Old PM Url: $oldPMUrl")
    println("New PM Url: $newPMUrl")

    println()
    println()

    testDataList.forEach { testData: TestData ->
        val oldPMResponse: ApiResponse = callPMApi(oldPMUrl, testData)
        val newPMResponse: ApiResponse = callPMApi(newPMUrl, testData)

        val leftLines = mapper.readTree(oldPMResponse.response).toPrettyString().lines()
        val rightLines = mapper.readTree(newPMResponse.response).toPrettyString().lines()

        val maxLeftWidth = leftLines.maxOf { it.length } + 15  // padding
        val maxLines = maxOf(leftLines.size, rightLines.size)

        val isMatched: Boolean = compareTwoResponse(oldPMResponse, newPMResponse)

        println("Is Matched: ${if (isMatched) "$GREEN$isMatched$RESET" else "$RED$isMatched$RESET"}")
        println("Test Data Description: ${testData.description}")

        val prettyHeader = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testData.header)
        println("Test Data Headers:\n$prettyHeader")

        println("Test Data Params: ${testData.pathVar}")

        println(String.format("%-${maxLeftWidth}s | %s", "Old PM Response", "New PM Response"))
        println("-".repeat(maxLeftWidth + 3 + 50))
        for (i in 0 until maxLines) {
            val leftLine = if (i < leftLines.size) leftLines[i] else ""
            val rightLine = if (i < rightLines.size) rightLines[i] else ""
            println(String.format("%-${maxLeftWidth}s | %s", leftLine, rightLine))
        }

        println()
        println()
    }
}

private fun readFileFromResources(): String {
    val resource = {}.javaClass.classLoader.getResource(testDataFileName)
        ?: throw IllegalArgumentException("Resource not found: $testDataFileName")
    return File(resource.toURI()).readText()
}

fun readFile() : List<TestData> {
    val content: String = readFileFromResources()
    return mapper.readValue(content)
}

private fun compareTwoResponse(oldPMResponse: ApiResponse, newPMResponse: ApiResponse) : Boolean{
    return oldPMResponse == newPMResponse
}

private fun callPMApi(url: String, testData: TestData): ApiResponse {
    val request = if (testData.method.equals("GET", ignoreCase = true)) {
        Request.Builder()
            .url(url + testData.pathVar)
            .headers(testData.header.toHeaders())
            .get() // GET must not have body
            .build()
    } else {
        val body = testData.body.toRequestBody(mediaType)
        Request.Builder()
            .url(url + testData.pathVar)
            .headers(testData.header.toHeaders())
            .method(testData.method.uppercase(), body) // POST, PUT, PATCH, etc.
            .build()
    }

    return client.newCall(request).execute().use { response ->
        ApiResponse(response.code, response.body?.string() ?: "")
    }
}

private fun getUnsafeOkHttpClient(): OkHttpClient {
    // Create a trust manager that does not validate certificate chains
    val trustAllCerts = arrayOf<TrustManager>(
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    )

    // Install the all-trusting trust manager
    val sslContext = SSLContext.getInstance("SSL")
    sslContext.init(null, trustAllCerts, SecureRandom())
    val sslSocketFactory = sslContext.socketFactory

    // Build the client
    return OkHttpClient.Builder()
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier(HostnameVerifier { _, _ -> true })
        .build()
}