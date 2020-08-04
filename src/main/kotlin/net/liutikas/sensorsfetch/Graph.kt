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
private val dataToGraph = listOf(
        "dht22" to "temperature",
        "dht22" to "humidity",
        "sds011" to "P1",
        "sds011" to "P2",
        "pms3003" to "P1",
        "pms3003" to "P2"
)

fun parseCsv(csvFile: File): CSVParser {
    val reader = FileReader(csvFile)
    val format = CSVFormat.newFormat(';').withFirstRecordAsHeader()
    return format.parse(reader)
}

fun generateGraph(successfullyFetchedFiles: Map<String, List<List<File>>>, outputDirectory: File) {
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