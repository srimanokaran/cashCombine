package com.example.cashCombine.ledger.imports

import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CommBankCsvParserTest {

    private lateinit var parser: CommBankCsvParser

    @BeforeEach
    fun setUp() {
        parser = CommBankCsvParser()
    }

    @Test
    fun `parses date amount description and balance`() {
        val row = parser.parseLine(
            "10/07/2026,\"-45.00\",\"WOOLWORTHS 1234 FAKETOWN VIC AUS Card xx0000 Value Date: 08/07/2026\",\"+2455.00\""
        )

        assertThat(row.date).isEqualTo(LocalDate.of(2026, 7, 10))
        assertThat(row.amount).isEqualByComparingTo(BigDecimal("-45.00"))
        assertThat(row.description).contains("WOOLWORTHS")
        assertThat(row.balance).isEqualByComparingTo(BigDecimal("2455.00"))
    }

    @Test
    fun `parses sample fixture`() {
        javaClass.getResourceAsStream("/csv/commbank-sample.csv")!!.use { input ->
            val rows = parser.parse(input)

            assertThat(rows).hasSize(8)
            assertThat(rows[0].amount).isEqualByComparingTo("1500.00")
            assertThat(rows[1].description).contains("WOOLWORTHS")
        }
    }

    @Test
    fun `throws when column count is wrong`() {
        assertThatThrownBy { parser.parseLine("10/07/2026,\"-45.00\",\"Only three columns\"") }
            .isInstanceOf(InvalidCsvRowException::class.java)
            .hasMessageContaining("Expected 4 columns")
    }

    @Test
    fun `throws when date is invalid`() {
        assertThatThrownBy { parser.parseLine("not-a-date,\"-45.00\",\"Description\",\"+100.00\"") }
            .isInstanceOf(InvalidCsvRowException::class.java)
            .hasMessageContaining("Could not parse row")
    }
}