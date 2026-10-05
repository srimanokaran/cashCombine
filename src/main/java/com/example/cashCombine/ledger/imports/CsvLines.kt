package com.example.cashCombine.ledger.imports

internal object CsvLines {

    /**
     * Splits one CSV line into fields, respecting quoted commas.
     *
     * Input:
     * `10/07/2026,"-45.00","WOOLWORTHS 1234, FAKETOWN","+2455.00"`
     *
     * Output (quotes removed, commas inside quotes kept):
     * `["10/07/2026", "-45.00", "WOOLWORTHS 1234, FAKETOWN", "+2455.00"]`
     */
    fun split(line: String): Array<String> {
        val fields = ArrayList<String>()
        val current = StringBuilder()
        var inQuotes = false

        for (character in line) {
            if (character == '"') {
                inQuotes = !inQuotes
                continue
            }
            if (character == ',' && !inQuotes) {
                fields.add(current.toString())
                current.clear()
                continue
            }
            current.append(character)
        }

        fields.add(current.toString())
        return fields.toTypedArray()
    }
}