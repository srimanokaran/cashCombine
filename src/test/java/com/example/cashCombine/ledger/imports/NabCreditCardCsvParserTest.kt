package com.example.cashCombine.ledger.imports

import java.math.BigDecimal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NabCreditCardCsvParserTest {

    private lateinit var parser: NabCreditCardCsvParser

    @BeforeEach
    fun setUp() {
        parser = NabCreditCardCsvParser()
    }

    @Test
    fun `parses purchase with Processed On encoded as balance`() {
        val row = parser.parseLine(
            "21 July 26,-11.13,Card ending 9999,,CREDIT CARD PURCHASE,EXAMPLE CAFE FAKETOWN VIC,Restaurants & takeaway,Example Cafe,21 July 26"
        )

        assertThat(row.date).isEqualTo(LocalDate.of(2026, 7, 21))
        assertThat(row.amount).isEqualByComparingTo(BigDecimal("-11.13"))
        assertThat(row.description).isEqualTo("EXAMPLE CAFE FAKETOWN VIC")
        assertThat(row.balance).isEqualByComparingTo(BigDecimal.valueOf(LocalDate.of(2026, 7, 21).toEpochDay()))
    }

    @Test
    fun `uses zero balance when Processed On blank`() {
        val row = parser.parseLine(
            "21 July 26,-3.66,Card ending 9999,,MISCELLANEOUS DEBIT,WOOLWORTHS 1234 FAKETOWN,Groceries,Woolworths Metro,"
        )

        assertThat(row.date).isEqualTo(LocalDate.of(2026, 7, 21))
        assertThat(row.amount).isEqualByComparingTo(BigDecimal("-3.66"))
        assertThat(row.description).isEqualTo("WOOLWORTHS 1234 FAKETOWN")
        assertThat(row.balance).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `parses abbreviated months including Sept`() {
        val row = parser.parseLine(
            "21 Sept 26,-7.99,Card ending 2001,,MISCELLANEOUS DEBIT,DAN MURPHY'S 3778 BRUNSWICK E,Alcohol,Dan Murphy's (Brunswick East),"
        )

        assertThat(row.date).isEqualTo(LocalDate.of(2026, 9, 21))
        assertThat(row.amount).isEqualByComparingTo(BigDecimal("-7.99"))
        assertThat(row.description).isEqualTo("DAN MURPHY'S 3778 BRUNSWICK E")
        assertThat(row.balance).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `keeps payment and refund signs`() {
        val payment = parser.parseLine(
            "04 July 26,702.79,0000000000000000000,,CREDIT CARD PAYMENT,BPAY PAYMENT - THANK YOU,Internal transfers,,06 July 26"
        )
        val refund = parser.parseLine(
            "29 June 26,12.66,Card ending 9999,,CREDIT CARD REFUND,UBER * EATS PENDING Example City AUS,Refund,Uber Eats,29 June 26"
        )

        assertThat(payment.amount).isEqualByComparingTo("702.79")
        assertThat(payment.description).isEqualTo("BPAY PAYMENT - THANK YOU")
        assertThat(refund.amount).isEqualByComparingTo("12.66")
        assertThat(refund.description).contains("UBER")
    }

    @Test
    fun `parses sample fixture and skips header`() {
        javaClass.getResourceAsStream("/csv/qantas-money-sample.csv")!!.use { input ->
            val rows = parser.parse(input)

            assertThat(rows).hasSize(7)
            assertThat(rows[0].description).contains("WOOLWORTHS")
            assertThat(rows[3].description).contains("BPAY PAYMENT")
        }
    }

    @Test
    fun `throws when column count is wrong`() {
        assertThatThrownBy { parser.parseLine("21 July 26,-3.66,only,three") }
            .isInstanceOf(InvalidCsvRowException::class.java)
            .hasMessageContaining("Expected 9 columns")
    }
}