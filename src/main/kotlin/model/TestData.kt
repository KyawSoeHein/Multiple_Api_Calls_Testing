package org.gic.model

import okhttp3.RequestBody

data class TestData(
    val description: String,
    val method: String,
    val header: Map<String, String>,
    val body: String
)