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
import java.io.File
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
    val successfullyFetchedFiles = startFetching(fetchConfig, outputDirectory, sensors)
    if (fetchConfig.dataToGraph.isEmpty()) {
        println("There was no entry in the config for data to graph")
        exitProcess(-1)
    }
    generateGraph(fetchConfig.dataToGraph, successfullyFetchedFiles, outputDirectory)
}

@JsonClass(generateAdapter = true)
data class FetchConfig(
       val days: Long = 1,
       val sensorsNames: List<String> = emptyList(),
       val dataToGraph: List<List<String>> = emptyList()
)