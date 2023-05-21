package ch.obermuhlner.util


object SimpleTokenizer {
    fun tokenize(input: String): List<String> {
        val tokens: MutableList<String> = ArrayList()
        var insideQuotes = false
        var currentToken = StringBuilder()
        for (c in input.toCharArray()) {
            if (c == '\"') {
                insideQuotes = !insideQuotes
            } else if (c == ' ' && !insideQuotes) {
                if (currentToken.isNotEmpty()) {
                    tokens.add(currentToken.toString())
                    currentToken = StringBuilder()
                }
            } else {
                currentToken.append(c)
            }
        }
        tokens.add(currentToken.toString())
        return tokens
    }
}