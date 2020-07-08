// Copyright 2020 Aurimas Liutikas
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package net.liutikas.sensorsfetch

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Script expects you to pass a path to the config")
        exitProcess(-1)
    }
    val configFile = File(args[0])
    val moshi = Moshi.Builder().build()
    val adapter = moshi.adapter(FetchConfig::class.java)
    val fetchConfig = adapter.fromJson(configFile.readText())
    if (fetchConfig == null) {
        println("Passed in invalid config $configFile")
        exitProcess(-1)
    }
    val sensors = fetchConfig.sensorsNames
    if (sensors.isEmpty()) {
        println("sensorsName was an empty list in $configFile")
        exitProcess(-1)
    }
    val client = OkHttpClient()
    val startDate = LocalDate.now()
    val endDate = startDate.minusDays(fetchConfig.days)
    for (sensor in sensors) {
        fetchDevice(client, startDate, endDate, sensor)
    }
}

fun fetchDevice(
        client: OkHttpClient,
        startDate: LocalDate,
        endDate: LocalDate,
        sensorName: String
) {
    print("Fetching ${sensorName} ")
    for (date in endDate..startDate) {
        if (!client.fetch(date.toString(), sensorName)) {
            println("\nFailed to fetch ${getUrl(date.toString(), sensorName)}")
            break
        }
        print(".")
    }
    println()
}

fun getUrl(date: String, sensorName: String): String {
    return "https://archive.sensor.community/${date}/${date}_${sensorName}.csv"
}

fun OkHttpClient.fetch(date: String, sensorName: String): Boolean {
    val outputFile = File("${date}_${sensorName}.csv")
    if (outputFile.exists()) return true
    val url = getUrl(date, sensorName)
    val request = Request.Builder().url(url).build()
    val response = newCall(request).execute()
    if (response.code != 200) return false
    val body = response.body?.byteStream() ?: return false
    Files.copy(body, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    return true
}

@JsonClass(generateAdapter = true)
data class FetchConfig(
       val days: Long = 1,
       val sensorsNames: List<String> = emptyList()
)