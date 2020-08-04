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
    if (args.isEmpty()) {
        println("Script expects you to pass a path to the config and optionally an output directory")
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
    val outputDirectory: File
    if (args.size > 1) {
        outputDirectory = File(args[1])
        if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
            println("Unable to create $outputDirectory")
            exitProcess(-1)
        }
    } else {
        outputDirectory = File(".")
    }
    val client = OkHttpClient()
    val startDate = LocalDate.now()
    val endDate = startDate.minusDays(fetchConfig.days)
    println("Downloading data between $startDate and $endDate. Logs will be placed in ${outputDirectory.canonicalPath}.\n")
    val successfullyFetchedFiles: MutableMap<String, MutableList<MutableList<File>>> = mutableMapOf()
    for (sensor in sensors.sorted()) {
        val sensorType = sensor.substringBefore('_')
        if (!successfullyFetchedFiles.containsKey(sensorType)) successfullyFetchedFiles[sensorType] = mutableListOf()
        val sensorList = successfullyFetchedFiles[sensorType]!!
        val sensorFiles = mutableListOf<File>()
        sensorList.add(sensorFiles)
        fetchDevice(client, startDate, endDate, sensor, outputDirectory, sensorFiles)
    }
    if (fetchConfig.dataToGraph.isEmpty()) {
        println("There was no entry in the config for data to graph")
        exitProcess(-1)
    }
    generateGraph(fetchConfig.dataToGraph, successfullyFetchedFiles, outputDirectory)
}

fun fetchDevice(
        client: OkHttpClient,
        startDate: LocalDate,
        endDate: LocalDate,
        sensorName: String,
        outputDirectory: File,
        successfullyFetchedFiles: MutableList<File>
) {
    println("Fetching $sensorName")
    for (date in endDate..startDate) {
        print("$date")
        val fetchedFile = client.fetch(date.toString(), sensorName, outputDirectory)
        if (fetchedFile == null) {
            println(" - failure. Failed to fetch ${getUrl(date.toString(), sensorName)}")
        } else {
            successfullyFetchedFiles.add(fetchedFile)
            println(" - success")
        }
    }
    println("---------------")
}

fun getUrl(date: String, sensorName: String): String {
    return "https://archive.sensor.community/${date}/${date}_${sensorName}.csv"
}

fun OkHttpClient.fetch(
        date: String,
        sensorName: String,
        outputDirectory: File
): File? {
    val outputFile = File(outputDirectory, "${date}_${sensorName}.csv")
    if (outputFile.exists()) return outputFile
    val url = getUrl(date, sensorName)
    val request = Request.Builder().url(url).build()
    val response = newCall(request).execute()
    if (response.code != 200) return null
    val body = response.body?.byteStream() ?: return null
    Files.copy(body, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    return outputFile
}

@JsonClass(generateAdapter = true)
data class FetchConfig(
       val days: Long = 1,
       val sensorsNames: List<String> = emptyList(),
       val dataToGraph: List<List<String>> = emptyList()
)