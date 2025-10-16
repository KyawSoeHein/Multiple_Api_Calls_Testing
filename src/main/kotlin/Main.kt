package org.gic

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.gic.model.ApiResponse
import org.gic.model.TestData
import java.io.File

val mapper = jacksonObjectMapper()
const val testDataFileName : String = "test_data.json"
val client: OkHttpClient = OkHttpClient()
val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()

fun main() {
    val testDataList : List<TestData> = readFile()

    testDataList.forEach { testData: TestData ->
        val oldPMResponse: ApiResponse = callPMApi(oldPMUrl, testDataList[0])
        val newPMResponse: ApiResponse = callPMApi(newPMUrl, testDataList[0])

        val isMatched: Boolean = compareTwoResponse(oldPMResponse, newPMResponse)

        println("Test Data: $testData")
        println("Is Matched: $isMatched")
        println(String.format("%-100s | %-100s", "Old PM Response", "New PM Response"))
        println(String.format("%-100s | %-100s", mapper.readTree(oldPMResponse.response).toPrettyString(), mapper.readTree(newPMResponse.response).toPrettyString()))
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
            .url(url)
            .get() // GET must not have body
            .build()
    } else {
        val body = testData.body.toRequestBody(mediaType)
        Request.Builder()
            .url(url)
            .method(testData.method.uppercase(), body) // POST, PUT, PATCH, etc.
            .build()
    }

    return client.newCall(request).execute().use { response ->
        ApiResponse(response.code, response.body?.string() ?: "")
    }
}