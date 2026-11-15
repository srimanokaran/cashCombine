package com.example.cashCombine.config

import com.example.cashCombine.ledger.search.HttpOllamaProbe
import com.example.cashCombine.ledger.search.LocalOllamaRuntime
import com.example.cashCombine.ledger.search.ProcessOllamaLauncher
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class LocalOllamaConfig {

    @Bean(destroyMethod = "close")
    fun localOllamaRuntime(
        @Value("\${search.ai.base-url:http://127.0.0.1:11434}") baseUrl: String,
        @Value("\${search.ai.model:llama3}") model: String,
        // Docker Compose runs Ollama as its own service, so only connect there.
        @Value("\${search.ai.start-ollama:true}") startOwned: Boolean,
    ): LocalOllamaRuntime = LocalOllamaRuntime(
        HttpOllamaProbe(baseUrl, model),
        ProcessOllamaLauncher(baseUrl),
        startOwned = startOwned,
        baseUrl = baseUrl,
        model = model,
    )
}
