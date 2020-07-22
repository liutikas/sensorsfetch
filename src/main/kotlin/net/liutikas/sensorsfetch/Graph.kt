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

fun parseCsv(csvPath: String): CSVParser {
    val reader = FileReader(csvPath)
    val format = CSVFormat.newFormat(';').withFirstRecordAsHeader()
    return format.parse(reader)
}

fun generateGraph2() {
    Plotly.page {
        title = "Fancy graph"
        plot {
            trace(getTrace("sds011_sensor_43258", "P1"))
            trace(getTrace("sds011_sensor_43295", "P1"))
            trace(getTrace("sds011_sensor_44016", "P1"))
            trace(getTrace("sds011_sensor_46879", "P1"))
            trace(getTrace("sds011_sensor_46881", "P1"))
            trace(getTrace("sds011_sensor_47325", "P1"))
            trace(getTrace("sds011_sensor_47449", "P1"))
            layout {
                title = "SDS011 P1"
            }
        }
    }.makeFile(File("sds001-p1.html"))

    Plotly.page {
        title = "Fancy graph"
        plot {
            trace(getTrace("sds011_sensor_43258", "P2"))
            trace(getTrace("sds011_sensor_43295", "P2"))
            trace(getTrace("sds011_sensor_44016", "P2"))
            trace(getTrace("sds011_sensor_46879", "P2"))
            trace(getTrace("sds011_sensor_46881", "P2"))
            trace(getTrace("sds011_sensor_47325", "P2"))
            trace(getTrace("sds011_sensor_47449", "P2"))
            layout {
                title = "SDS011 P2"
            }
        }
    }.makeFile(File("sds001-p2.html"))

    Plotly.page {
        title = "Fancy graph"
        plot {
            trace(getTrace("dht22_sensor_43259", "temperature"))
            trace(getTrace("dht22_sensor_43296", "temperature"))
            trace(getTrace("dht22_sensor_44017", "temperature"))
            trace(getTrace("dht22_sensor_46880", "temperature"))
            trace(getTrace("dht22_sensor_46882", "temperature"))
            trace(getTrace("dht22_sensor_47326", "temperature"))
            trace(getTrace("dht22_sensor_47450", "temperature"))

            layout {
                title = "DHT22 temperature"
            }
        }
    }.makeFile(File("dht22-temperature.html"))
}

fun getTrace(name: String, column: String): Trace {
    val records = parseCsv("output/2020-07-06_$name.csv")
    val dates = mutableListOf<Long>()
    val p1 = mutableListOf<Double>()
    for (record in records) {
        dates.add(dateFormat.parse(record.get("timestamp")).time)
        p1.add(record.get(column).toDouble())
    }
    val trace = Trace.build(dates, p1)
    trace.name = name
    return trace
}