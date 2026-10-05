package com.example.cashCombine.infrastructure.persistence

import com.example.cashCombine.ledger.categorisation.CategoryAssignmentSource
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "transactions",
    uniqueConstraints =
        [UniqueConstraint(
            name = "uk_transactions_fingerprint",
            columnNames = ["account_id", "tx_date", "amount", "description", "balance"])],
    indexes = [
        Index(name = "idx_transactions_account", columnList = "account_id"),
        Index(name = "idx_transactions_import_batch", columnList = "import_batch_id")]
)
class TransactionJpaEntity {

    @Id
    var id: UUID? = null

    @Column(name = "account_id", nullable = false)
    var accountId: UUID? = null

    @Column(name = "import_batch_id")
    var importBatchId: UUID? = null

    @Column(name = "tx_date", nullable = false)
    var date: LocalDate? = null

    @Column(nullable = false, precision = 19, scale = 4)
    var amount: BigDecimal? = null

    @Column(nullable = false, length = 1024)
    var description: String? = null

    @Column(nullable = false, precision = 19, scale = 4)
    var balance: BigDecimal? = null

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "category_assignment_source", nullable = false)
    var categoryAssignmentSource: CategoryAssignmentSource? = null

    protected constructor()

    constructor(
        id: UUID?,
        accountId: UUID?,
        importBatchId: UUID?,
        date: LocalDate?,
        amount: BigDecimal?,
        description: String?,
        balance: BigDecimal?,
        categoryId: UUID?,
        categoryAssignmentSource: CategoryAssignmentSource?
    ) {
        this.id = id
        this.accountId = accountId
        this.importBatchId = importBatchId
        this.date = date
        this.amount = amount
        this.description = description
        this.balance = balance
        this.categoryId = categoryId
        this.categoryAssignmentSource = categoryAssignmentSource
    }
}