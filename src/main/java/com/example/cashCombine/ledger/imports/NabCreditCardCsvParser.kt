package com.example.cashCombine.ledger.imports

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Parses Qantas Money / NAB credit card CSV exports.
 *
 * Header:
 * `Date,Amount,Account Number,,Transaction Type,Transaction Details,Category,Merchant Name,Processed On`
 *
 * Amounts are already signed (spend negative, payment/refund positive).
 * `Processed On` is encoded into [ParsedTransactionRow.balance] as epoch-day for fingerprinting
 * when present; otherwise `0`.
 */
class NabCreditCardCsvParser : TransactionCsvParser {

    override fun parse(input: InputStream): List<ParsedTransactionRow> {
        return try {
            BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8)).use { reader ->
                parse(reader)
            }
        } catch (e: Exception) {
            throw RuntimeException("Failed to parse CSV", e)
        }
    }

    fun parse(bufferedReader: BufferedReader): List<ParsedTransactionRow> {
        val rows = ArrayList<ParsedTransactionRow>()
        var firstNonBlank = true

        var line = bufferedReader.readLine()
        while (line != null) {
            if (line.isBlank()) {
                line = bufferedReader.readLine()
                continue
            }
            if (firstNonBlank) {
                firstNonBlank = false
                if (isHeader(line)) {
                    line = bufferedReader.readLine()
                    continue
                }
            }
            rows.add(parseLine(line))
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

        try {
            val date = parseDate(fields[0])
            val amount = BigDecimal(fields[1].trim())
            val description = fields[5].trim()
            if (description.isEmpty()) {
                throw InvalidCsvRowException("Transaction details are required: $line")
            }
            val balance = encodeProcessedOn(fields[8])
            return ParsedTransactionRow(date, amount, description, balance)
        } catch (ex: InvalidCsvRowException) {
            throw ex
        } catch (ex: Exception) {
            throw InvalidCsvRowException("Could not parse row: $line", ex)
        }
    }

    override fun shouldSkipLine(line: String): Boolean = isHeader(line)

    companion object {
        private const val EXPECTED_COLUMN_COUNT = 9
        /** Full month names, e.g. `21 July 26`. */
        private val FULL_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yy", Locale.ENGLISH)
        /** Abbreviated months, e.g. `21 Sep 26`. */
        private val SHORT_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yy", Locale.ENGLISH)

        private fun isHeader(line: String): Boolean {
            val fields = CsvLines.split(line)
            return fields.isNotEmpty() && "Date".equals(fields[0].trim(), ignoreCase = true)
        }

        private fun encodeProcessedOn(raw: String?): BigDecimal {
            val trimmed = raw?.trim().orEmpty()
            if (trimmed.isEmpty()) {
                return BigDecimal.ZERO
            }
            return BigDecimal.valueOf(parseDate(trimmed).toEpochDay())
        }

        /**
         * Accepts full month names (`July`) and abbreviations (`Sep`).
         * Qantas uses `Sept` for September, which Java's `MMM` does not recognise.
         */
        private fun parseDate(raw: String): LocalDate {
            val trimmed = raw.trim()
            try {
                return LocalDate.parse(trimmed, FULL_MONTH)
            } catch (_: Exception) {
                // try abbreviated form
            }
            val normalised = trimmed.replace("(?i)\\bSept\\b".toRegex(), "Sep")
            return LocalDate.parse(normalised, SHORT_MONTH)
        }
    }
}