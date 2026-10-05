package com.example.cashCombine.config

import org.hibernate.community.dialect.SQLiteDialect
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class JpaConfig {

    @Bean
    fun sqliteDialectCustomizer(): HibernatePropertiesCustomizer =
        HibernatePropertiesCustomizer { props -> props["hibernate.dialect"] = SQLiteDialect::class.java }
}