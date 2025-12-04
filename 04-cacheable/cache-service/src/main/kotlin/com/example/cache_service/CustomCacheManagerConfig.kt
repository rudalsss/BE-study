package com.example.cache_service

import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CustomCacheManagerConfig(
    private val customCache: Cache
) {
    @Bean
    fun cacheManager(): CacheManager {
        return object: CacheManager {
            override fun getCache(name: String): Cache? {
                return if (name == "customCache") customCache else null
            }

            override fun getCacheNames(): MutableCollection<String> {
                return mutableListOf("customCache")
            }
        }
    }

}