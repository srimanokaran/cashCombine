package com.example.cashCombine.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "classification_rules")
class ClassificationRuleJpaEntity {

    @Id
    var id: UUID? = null

    @Column(nullable = false)
    var pattern: String? = null

    @Column(name = "category_id", nullable = false)
    var categoryId: UUID? = null

    @Column(name = "created_order", nullable = false)
    var createdOrder: Long = 0

    protected constructor()

    constructor(id: UUID?, pattern: String?, categoryId: UUID?, createdOrder: Long) {
        this.id = id
        this.pattern = pattern
        this.categoryId = categoryId
        this.createdOrder = createdOrder
    }
}