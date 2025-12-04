package com.example.cache_service

import org.springframework.cache.CacheManager
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
class CacheController(
    private val cacheManager: CacheManager
) {
    @GetMapping("/cache")
    fun getCache(@RequestParam key: String): Any? {
        val cache = cacheManager.getCache("customCache")
        return cache?.get(key)?.get()
    }

    @PutMapping("/cache")
    fun putCache(
        @RequestParam key: String,
        @RequestParam value: String
    ): String {
        val cache = cacheManager.getCache("customCache")
        cache?.put(key, value)
        return "stored"
    }

    @DeleteMapping("/evict")
    fun evictCache(@RequestParam key: String): String {
        val cache = cacheManager.getCache("customCache")
        cache?.evict(key)
        return "evicted"
    }
}