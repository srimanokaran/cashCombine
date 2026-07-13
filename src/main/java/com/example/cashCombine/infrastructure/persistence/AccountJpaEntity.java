package com.example.cashCombine.infrastructure.persistence;

import com.example.cashCombine.ledger.accounts.AccountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "accounts")
public class AccountJpaEntity {

	@Id
	private UUID id;

	@Column(nullable = false)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AccountType type;

	@Column(name = "has_imports", nullable = false)
	private boolean hasImports;

	protected AccountJpaEntity() {
	}

	public AccountJpaEntity(UUID id, String name, AccountType type, boolean hasImports) {
		this.id = id;
		this.name = name;
		this.type = type;
		this.hasImports = hasImports;
	}

	public UUID getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public AccountType getType() {
		return type;
	}

	public boolean isHasImports() {
		return hasImports;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setType(AccountType type) {
		this.type = type;
	}

	public void setHasImports(boolean hasImports) {
		this.hasImports = hasImports;
	}

}
