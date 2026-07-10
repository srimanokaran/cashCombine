package com.example.cashCombine.config;

import org.hibernate.community.dialect.SQLiteDialect;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JpaConfig {

	@Bean
	public HibernatePropertiesCustomizer sqliteDialectCustomizer() {
		return (properties) -> properties.put("hibernate.dialect", SQLiteDialect.class);
	}

}
