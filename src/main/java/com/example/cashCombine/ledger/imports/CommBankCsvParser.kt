package com.example.cashCombine.ledger.imports

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CommBankCsvParser : TransactionCsvParser {

    /**
     * Parses a CommBank CSV export (no header row).
     *
     * Input line shape (exactly 4 columns):
     * `10/07/2026,"-45.00","WOOLWORTHS 1234 FAKETOWN VIC AUS","+2455.00"`
     *
     * Columns: date (DD/MM/YYYY), signed amount, description, signed balance.
     *
     * Output shape ([ParsedTransactionRow]):
     * `date=2026-07-10, amount=-45.00, description="WOOLWORTHS...", balance=2455.00`
     */
    override fun parse(input: InputStream): List<ParsedTransactionRow> {
        return try {
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                parse(reader)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse CSV", e)
        }
    }

    /**
     * Same input/output shape as [parse].
     * Does not close `bufferedReader` — caller owns the resource.
     */
    fun parse(bufferedReader: BufferedReader): List<ParsedTransactionRow> {
        val rows = ArrayList<ParsedTransactionRow>()

        var line = bufferedReader.readLine()
        while (line != null) {
            if (line.isNotBlank()) {
                rows.add(parseLine(line))
            }
            line = bufferedReader.readLine()
        }

        return rows
    }

    override fun parseLine(line: String): ParsedTransactionRow {
        val fields = CsvLines.split(line)
        if (fields.size != EXPECTED_COLUMN_COUNT) {
            throw InvalidCsvRowException(
                "Expected %d columns but found %d".format(EXPECTED_COLUMN_COUNT, fields.size)
            )
        }

        return try {
            val date = LocalDate.parse(fields[0].trim(), DATE_FORMAT)
            val amount = parseSignedAmount(fields[1])
            val description = fields[2].trim()
            val balance = parseSignedAmount(fields[3])
            ParsedTransactionRow(date, amount, description, balance)
        } catch (ex: Exception) {
            throw InvalidCsvRowException("Could not parse row: $line", ex)
        }
    }

    companion object {
        private const val EXPECTED_COLUMN_COUNT = 4
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        private fun parseSignedAmount(raw: String): BigDecimal {
            val normalized = raw.trim().replace("\"", "")
            return BigDecimal(normalized)
        }
    }
}