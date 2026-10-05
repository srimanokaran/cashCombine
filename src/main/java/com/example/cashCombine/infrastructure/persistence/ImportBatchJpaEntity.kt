package com.example.cashCombine.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "import_batches", indexes = [Index(name = "idx_import_batches_account", columnList = "account_id")])
class ImportBatchJpaEntity {

    @Id
    var id: UUID? = null

    @Column(name = "account_id", nullable = false)
    var accountId: UUID? = null

    @Column(length = 512)
    var filename: String? = null

    @Column(name = "imported_at", nullable = false)
    var importedAt: Instant? = null

    @Column(nullable = false)
    var accepted: Int = 0

    @Column(nullable = false)
    var duplicate: Int = 0

    @Column(nullable = false)
    var rejected: Int = 0

    protected constructor()

    constructor(
        id: UUID?,
        accountId: UUID?,
        filename: String?,
        importedAt: Instant?,
        accepted: Int,
        duplicate: Int,
        rejected: Int
    ) {
        this.id = id
        this.accountId = accountId
        this.filename = filename
        this.importedAt = importedAt
        this.accepted = accepted
        this.duplicate = duplicate
        this.rejected = rejected
    }
}