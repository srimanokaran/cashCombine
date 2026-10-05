package com.example.cashCombine.ledger.accounts

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class AccountTest {

    @Test
    fun `cannot change type after imports exist`() {
        val account = Account.create("CommBank Everyday", AccountType.COMMBANK)
        account.markAsImported()

        assertThatThrownBy { account.changeType(AccountType.COMMBANK) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("after imports")
    }
}