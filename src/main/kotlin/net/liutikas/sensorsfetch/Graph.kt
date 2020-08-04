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

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import scientifik.plotly.Plotly
import scientifik.plotly.makeFile
import scientifik.plotly.models.Trace
import java.io.File
import java.io.FileReader
import java.text.SimpleDateFormat

private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss"
private val dateFormat = SimpleDateFormat(DATE_FORMAT_PATTERN)

fun parseCsv(csvFile: File): CSVParser {
    val reader = FileReader(csvFile)
    val format = CSVFormat.newFormat(';').withFirstRecordAsHeader()
    return format.parse(reader)
}

fun generateGraph(
        dataToGraph: List<List<String>>,
        successfullyFetchedFiles: Map<String, List<List<File>>>,
        outputDirectory: File
) {
    for ((sensorType, entry) in dataToGraph) {
        val sensorList = successfullyFetchedFiles[sensorType] ?: return
        val htmlFile = File(outputDirectory, "$sensorType-$entry.html")
        Plotly.page {
            title = "Fancy graph"
            plot {
                for (sensor in sensorList) {
                    if (sensor.isEmpty()) continue
                    trace(getTrace(sensor, entry))
                }
                layout {
                    title = "$sensorType $entry"
                }
            }
        }.makeFile(htmlFile)
        println("Generated graph ${htmlFile.canonicalPath}")
    }
}

fun getTrace(fileList: List<File>, column: String): Trace {
    val values = mutableListOf<Pair<Long, Double>>()
    val name = fileList[0].name.substringBefore('.').substringAfter('_')
    for (file in fileList) {
        val records = parseCsv(file)
        for (record in records) {
            values.add(
                    dateFormat.parse(record.get("timestamp")).time to record.get(column).toDouble()
            )
        }
    }
    values.sortBy {(timestamp, _) -> timestamp}
    val dates = mutableListOf<Long>()
    val p1 = mutableListOf<Double>()
    for (pair in values) {
        dates.add(pair.first)
        p1.add(pair.second)
    }
    val trace = Trace.build(dates, p1)
    trace.name = name
    return trace
}