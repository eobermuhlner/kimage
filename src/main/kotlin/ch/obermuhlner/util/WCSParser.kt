package ch.obermuhlner.util

import java.io.File

object WCSParser {

    private val patternQuoted = "^\\s*(\\S+)\\s*=\\s*'([^']+)'".toRegex()
    private val patternSimple = "^\\s*(\\S+)\\s*=\\s*(\\S+)".toRegex()

    fun parse(file: File): Map<String, String> {
        return parse(file.readLines())
    }

    fun parse(lines: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        lines.forEach { line ->
            val matchQuoted = patternQuoted.find(line)
            if (matchQuoted != null) {
                result[matchQuoted.groupValues[1]] = matchQuoted.groupValues[2]
            } else {
                val matchSimple = patternSimple.find(line)
                if (matchSimple != null) {
                    result[matchSimple.groupValues[1]] = matchSimple.groupValues[2]
                }
            }
        }

        return result
    }
}