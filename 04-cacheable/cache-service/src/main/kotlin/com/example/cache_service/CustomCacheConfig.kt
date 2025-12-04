package com.example.cache_service

import org.springframework.cache.Cache
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap

@Configuration
class CustomCacheConfig {

    @Bean(name = ["customCache"])
    fun customCache(): Cache {
        val store = ConcurrentHashMap<Any, Any?>()

        return object : Cache {
            override fun getName(): String {
                return "customCache"
            }

            override fun getNativeCache(): Any {
                return store
            }

            override fun get(key: Any): Cache.ValueWrapper? {
                val value = store[key]
                if(value == null) return null
                return Cache.ValueWrapper{value}
            }

            override fun <T : Any?> get(key: Any, type: Class<T>?): T? {
                val value = store[key] ?: return null
                if (type == null) {
                    return value as T
                }
                return type.cast(value)
            }

            override fun <T : Any?> get(key: Any, valueLoader: Callable<T>): T? {
                val value = store[key]
                if (value != null) {
                    return value as T
                }

                val loaded = valueLoader.call()
                store[key] = loaded
                return loaded as T
            }

            override fun put(key: Any, value: Any?) {
                store[key] = value
            }

            override fun evict(key: Any) {
                store.remove(key)
            }

            override fun clear() {
                store.clear()
            }
        }
    }
}