package com.example.cashCombine.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "categories")
class CategoryJpaEntity {

    @Id
    var id: UUID? = null

    @Column(nullable = false, unique = true)
    var name: String? = null

    protected constructor()

    constructor(id: UUID?, name: String?) {
        this.id = id
        this.name = name
    }
}