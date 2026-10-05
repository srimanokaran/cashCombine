package com.example.cashCombine.ledger.imports

import java.io.InputStream

interface TransactionCsvParser {

    fun parse(input: InputStream): List<ParsedTransactionRow>

    fun parseLine(line: String): ParsedTransactionRow

    /** Header or other non-data lines that should be ignored during import. */
    fun shouldSkipLine(line: String): Boolean = false
}