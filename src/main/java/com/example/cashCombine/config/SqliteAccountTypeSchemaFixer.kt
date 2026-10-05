package com.example.cashCombine.config

import javax.sql.DataSource
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component

@Component
@Order(0)
class SqliteAccountTypeSchemaFixer(private val dataSource: DataSource) : ApplicationRunner {

    private val log = LoggerFactory.getLogger("api")

    override fun run(args: ApplicationArguments) {
        dataSource.connection.use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA busy_timeout = 10000")
                statement.execute("PRAGMA journal_mode = WAL")

                if (!tableExists(statement, "accounts")) {
                    return
                }

                val schema = tableSql(statement, "accounts")
                if (schema == null || !schema.contains("type in ('COMMBANK')") || schema.contains("NAB_CREDIT_CARD")) {
                    return
                }

                log.info("Migrating accounts table to allow CommBank / ING / NAB credit card types")
                statement.execute("BEGIN IMMEDIATE")
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
                        """.trimIndent()
                    )
                    statement.execute(
                        "INSERT INTO accounts_new (id, has_imports, name, type) SELECT id, has_imports, name, type FROM accounts"
                    )
                    statement.execute("DROP TABLE accounts")
                    statement.execute("ALTER TABLE accounts_new RENAME TO accounts")
                    statement.execute("COMMIT")
                } catch (ex: Exception) {
                    statement.execute("ROLLBACK")
                    throw ex
                }
            }
        }
    }

    companion object {
        private fun tableExists(statement: java.sql.Statement, table: String): Boolean {
            statement.executeQuery(
                "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = '$table'"
            ).use { rs -> return rs.next() }
        }

        private fun tableSql(statement: java.sql.Statement, table: String): String? {
            statement.executeQuery(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = '$table'"
            ).use { rs ->
                if (!rs.next()) {
                    return null
                }
                return rs.getString(1)
            }
        }
    }
}