package com.example.cashCombine.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "classification_rules")
public class ClassificationRuleJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String pattern;

	@Column(name = "category_id", nullable = false)
	private UUID categoryId;

	@Column(name = "created_order", nullable = false)
	private long createdOrder;

	protected ClassificationRuleJpaEntity() {
	}

	public ClassificationRuleJpaEntity(UUID id, String pattern, UUID categoryId, long createdOrder) {
		this.id = id;
		this.pattern = pattern;
		this.categoryId = categoryId;
		this.createdOrder = createdOrder;
	}

	public UUID getId() {
		return id;
	}

	public String getPattern() {
		return pattern;
	}

	public UUID getCategoryId() {
		return categoryId;
	}

	public long getCreatedOrder() {
		return createdOrder;
	}

}
