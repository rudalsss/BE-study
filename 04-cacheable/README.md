# 🚀 Spring Custom Cache – 직접 구현하며 캐싱 구조 이해하기
## 🎯 학습 목표
- Spring Cache 추상화의 핵심 구조를 직접 구현하며 이해한다.
- Cache 인터페이스의 메서드 동작(get/put/evict/clear)을 직접 작성해 본다.
- 커스텀 CacheManager를 구현해 스프링 캐싱 기능의 전체 흐름을 파악한다.
- Controller를 통해 캐시 저장/조회/삭제 동작을 검증해본다.
## 📦 환경 세팅
#### CustomCacheConfig – Cache 자체 구현
- 내부 저장소로 ConcurrentHashMap<Any, Any?> 사용
- getName()
- getNativeCache()
- get(key)
- get(key, Class<T>)
- get(key, Callable<T>)
- put(key, value)
- evict(key)
- clear()
#### CustomCacheManagerConfig – CacheManager 구현
- 특정 이름(customCache)을 가진 Cache만 관리
- getCache(name) → customCache 반환
- getCacheNames() → [“customCache”]
#### CacheController – 기능 테스트용 REST API 제공
- GET /api/v1/cache?key=
- PUT /api/v1/cache?key=&value=
- DEL /api/v1/evict?key=
