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

import kotlinx.coroutines.*
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.gildor.coroutines.okhttp.await
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.LocalDate

fun startFetching(
        fetchConfig: FetchConfig,
        outputDirectory: File,
        sensors: List<String>
): Map<String, List<List<File>>> {
    val client = OkHttpClient()
    val startDate = LocalDate.now()
    val endDate = startDate.minusDays(fetchConfig.days)
    println("Downloading data between $startDate and $endDate. Logs will be placed in ${outputDirectory.canonicalPath}.\n")
    val successfullyFetchedFiles: MutableMap<String, MutableList<MutableList<File>>> = mutableMapOf()
    for (sensor in sensors.sorted()) {
        val sensorType = sensor.substringBefore('_')
        if (!successfullyFetchedFiles.containsKey(sensorType)) {
            successfullyFetchedFiles[sensorType] = mutableListOf()
        }
        val sensorList = successfullyFetchedFiles[sensorType]!!
        val sensorFiles = mutableListOf<File>()
        sensorList.add(sensorFiles)
        fetchDevice(client, startDate, endDate, sensor, outputDirectory, sensorFiles)
    }
    client.dispatcher.executorService.shutdown()
    return successfullyFetchedFiles
}

private fun fetchDevice(
        client: OkHttpClient,
        startDate: LocalDate,
        endDate: LocalDate,
        sensorName: String,
        outputDirectory: File,
        successfullyFetchedFiles: MutableList<File>
) {
    println("Fetching $sensorName")
    runBlocking {
        val deferreds: MutableList<Deferred<FetchResult>> = mutableListOf()
        for (date in endDate..startDate) {
            deferreds.add(async {
                client.fetch(date.toString(), sensorName, outputDirectory)
            })
        }
        deferreds.awaitAll().sortedBy { it.date }.forEach { result ->
            print(result.date)
            if (result is SuccessfulResult) {
                successfullyFetchedFiles.add(result.file)
                println(" - success")
            } else {
                println(" - failure. Failed to fetch ${getUrl(result.date, sensorName)}")

            }
        }
    }
    println("---------------")
}

private sealed class FetchResult(val date: String)
private class SuccessfulResult(date: String, val file: File): FetchResult(date)
private class FailedResult(date: String): FetchResult(date)

private fun getUrl(date: String, sensorName: String): String {
    return "https://archive.sensor.community/${date}/${date}_${sensorName}.csv"
}

private suspend fun OkHttpClient.fetch(
        date: String,
        sensorName: String,
        outputDirectory: File
): FetchResult {
    val outputFile = File(outputDirectory, "${date}_${sensorName}.csv")
    if (outputFile.exists()) return SuccessfulResult(date, outputFile)
    val url = getUrl(date, sensorName)
    val request = Request.Builder().url(url).build()
    newCall(request).await().use { response ->
        if (response.code != 200) return FailedResult(date)
        val body = response.body?.byteStream() ?: return FailedResult(date)
        writeToFile(body, outputFile)
        return SuccessfulResult(date, outputFile)
    }
}

private suspend fun writeToFile(body: InputStream, outputFile: File) {
    withContext(Dispatchers.IO) {
        Files.copy(body, outputFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
}