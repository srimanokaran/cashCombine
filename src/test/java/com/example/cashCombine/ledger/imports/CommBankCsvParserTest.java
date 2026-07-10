package com.example.cashCombine.ledger.imports;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommBankCsvParserTest {

	private CommBankCsvParser parser;

	@BeforeEach
	void setUp() {
		parser = new CommBankCsvParser();
	}

	@Test
	void parsesDateAmountDescriptionAndBalance() {
		var row = parser.parseLine(
				"10/07/2026,\"-45.00\",\"WOOLWORTHS 1234 FAKETOWN VIC AUS Card xx0000 Value Date: 08/07/2026\",\"+2455.00\"");

		assertThat(row.date()).isEqualTo(LocalDate.of(2026, 7, 10));
		assertThat(row.amount()).isEqualByComparingTo(new BigDecimal("-45.00"));
		assertThat(row.description()).contains("WOOLWORTHS");
		assertThat(row.balance()).isEqualByComparingTo(new BigDecimal("2455.00"));
	}

	@Test
	void parsesSampleFixture() throws Exception {
		try (var input = getClass().getResourceAsStream("/csv/commbank-sample.csv")) {
			var rows = parser.parse(input);

			assertThat(rows).hasSize(8);
			assertThat(rows.get(0).amount()).isEqualByComparingTo("1500.00");
			assertThat(rows.get(1).description()).contains("WOOLWORTHS");
		}
	}

	@Test
	void throwsWhenColumnCountIsWrong() {
		assertThatThrownBy(() -> parser.parseLine("10/07/2026,\"-45.00\",\"Only three columns\""))
				.isInstanceOf(InvalidCsvRowException.class)
				.hasMessageContaining("Expected 4 columns");
	}

	@Test
	void throwsWhenDateIsInvalid() {
		assertThatThrownBy(() -> parser.parseLine("not-a-date,\"-45.00\",\"Description\",\"+100.00\""))
				.isInstanceOf(InvalidCsvRowException.class)
				.hasMessageContaining("Could not parse row");
	}

}
