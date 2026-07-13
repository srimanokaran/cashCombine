package com.example.cashCombine.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ApiReadyLogger {

	private static final Logger log = LoggerFactory.getLogger("api");

	private final Environment environment;

	public ApiReadyLogger(Environment environment) {
		this.environment = environment;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onReady() {
		String port = environment.getProperty("local.server.port", environment.getProperty("server.port", "8080"));
		log.info("cashCombine ready → http://localhost:{}", port);
		log.info("Logging /api/* calls (4xx/5xx show rejection reason)");
	}

}
