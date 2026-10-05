package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.accounts.AccountType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "accounts")
class AccountJpaEntity {

    @Id
    var id: UUID? = null

    @Column(nullable = false)
    var name: String? = null

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var type: AccountType? = null

    @Column(name = "has_imports", nullable = false)
    var hasImports: Boolean = false

    protected constructor()

    constructor(id: UUID?, name: String?, type: AccountType?, hasImports: Boolean) {
        this.id = id
        this.name = name
        this.type = type
        this.hasImports = hasImports
    }
}