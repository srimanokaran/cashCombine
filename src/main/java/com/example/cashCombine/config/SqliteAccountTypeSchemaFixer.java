package com.example.cashCombine.config;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Hibernate {@code ddl-auto=update} does not widen SQLite enum CHECK constraints.
 * Rebuild {@code accounts} when it still only allows {@code COMMBANK}.
 */
@Component
@Order(0)
public class SqliteAccountTypeSchemaFixer implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger("api");

	private final DataSource dataSource;

	public SqliteAccountTypeSchemaFixer(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("PRAGMA busy_timeout = 10000");
			statement.execute("PRAGMA journal_mode = WAL");

			if (!tableExists(statement, "accounts")) {
				return;
			}

			String schema = tableSql(statement, "accounts");
			if (schema == null || !schema.contains("type in ('COMMBANK')") || schema.contains("NAB_CREDIT_CARD")) {
				return;
			}

			log.info("Migrating accounts table to allow CommBank / ING / NAB credit card types");
			statement.execute("BEGIN IMMEDIATE");
			try {
				statement.execute(
						"""
						CREATE TABLE accounts_new (
							id blob not null,
							has_imports boolean not null,
							name varchar(255) not null,
							type varchar(255) not null,
							primary key (id)
						)
						""");
				statement.execute(
						"INSERT INTO accounts_new (id, has_imports, name, type) SELECT id, has_imports, name, type FROM accounts");
				statement.execute("DROP TABLE accounts");
				statement.execute("ALTER TABLE accounts_new RENAME TO accounts");
				statement.execute("COMMIT");
			} catch (Exception ex) {
				statement.execute("ROLLBACK");
				throw ex;
			}
		}
	}

	private static boolean tableExists(Statement statement, String table) throws Exception {
		try (ResultSet rs = statement.executeQuery(
				"SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '" + table + "'")) {
			return rs.next();
		}
	}

	private static String tableSql(Statement statement, String table) throws Exception {
		try (ResultSet rs = statement.executeQuery(
				"SELECT sql FROM sqlite_master WHERE type = 'table' AND name = '" + table + "'")) {
			if (!rs.next()) {
				return null;
			}
			return rs.getString(1);
		}
	}

}
