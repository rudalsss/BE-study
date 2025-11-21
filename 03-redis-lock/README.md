# 🚀 쿠폰발급시스템 – 동시성 제어 & Redis Atomic Operation 실험
## 🎯 학습 목표
- DB Lock(Pessimistic/Optimistic) 과 Redis Atomic Operation(LPOP, RPUSH) 을 비교한다.
- DB Lock(Pessimistic/Optimistic) 과 Redis Atomic Operation(LPOP) 을 비교한다.
- 외부 API 실패를 고려한 보상 트랜잭션(재고 복원) 전략을 적용한다.
- JMeter를 통한 실전 부하 테스트(TPS 1500) 로 안정성을 검증한다.
## 📦 환경 세팅
#### [ CouponController ] : 쿠폰재고관리
- /api/v1/coupons/add : 단건, 복수건 쿠폰 추가
- /api/v1/coupons/{couponId} : 쿠폰 코드 수정
- /api/v1/generate : 대규모 쿠폰 세팅(1000장)
#### [ CouponIssueController ] : 쿠폰발급관리
- /api/v1/coupons/{userId}/issue : 쿠폰발급
#### 레디스 세팅
- 로컬 도커 레디스 실행후 활용
- 유저 발급 쿠폰 Key: `rudals:user:coupon:{userId}`
- 재고 쿠폰 리스트 Key: `rudals:coupon:available`
---
## 1️⃣ 유저 쿠폰 발급 Caching (재고 없음)
- Redis에 발급된 쿠폰( `rudals:user:coupon:{userId}` )이 이미 존재하면
  ➡️ Already 상태 :: DB에서 쿠폰 찾고 반환
- 존재하지 않으면
  ➡️ Valid 상태 :: generate 쿠폰코드로 쿠폰 생성 → DB에저장, 유저쿠폰 Redis에 발급코드 저장 → 반환
  - 별도의 동시성 이슈 없음(재고없음)
- 별도의 inValid상태 없음

## 2️⃣ DB Lock 제거 후 Redis Atomic 재고 관리 도입
재고 관리는 Redis List로 대체(`rudals:coupon:available`)
- 재고추가 : RPUSH
- 재고발급 : LPOP(원자적)

재고추가시
```kotlin
fun pushAllToCouponRedis() {
    val codes = couponRepository.findAll().map { it.code } // 저장된 모든 쿠폰코드들
    redisTemplate.delete(INVENTORY_KEY) // 기존 인벤토리 삭제후 (중복 방지)
    redisTemplate.opsForList().rightPushAll(INVENTORY_KEY, codes) // 모두 저장
}
```
쿠폰발급시
```kotlin
private val USER_KEY = "rudals:user:coupon:"
private val INVENTORY_KEY = "rudals:coupon:available"

fun issueCoupon(userId: Long): Coupon? {
    val userKey = "$USER_KEY$userId"

    // 1. Already 체크
    val existing = redisTemplate.opsForValue().get(userKey)
    if (existing != null) return couponRepository.findByCode(existing)

    // 2. 재고 LPOP
    val couponCode = redisTemplate.opsForList().leftPop(INVENTORY_KEY)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "재고 소진")

    // 3. 발급 저장
    redisTemplate.opsForValue().set(userKey, couponCode)

    // 4. DB 조회 후 반환
    return couponRepository.findByCode(couponCode)
        ?: error("쿠폰이 DB에 없음")
}
```
📊 JMeter(1500TPS) 테스트 결과
| 항목    | 값                 |
| ----- | ----------------- |
| 요청 1500 | 모두 성공 송신 |
| Error% | 33.33%(재고부족 500건) |
| 중복발급  | 없음   |
| Redis재고 | 정확히 1000장 소비  |
➡️ 결론 : LPOP은 Atomic → 동시에 1000명이상 요청해도 중복 발급 및 충돌 없었다.

## 3️⃣ 재고 보상 트랜잭션 추가 (외부 API 실패 시 복원)
#### 문제상황 : 재고 보상 트랜잭션 추가 (외부 API 실패 시 복원)
🧪 실험조건 : MockConvenienceStoreUtil를 만들어 활용
```
MockConvenienceStoreUtil.sendCouponCodeToStore(couponCode) // 10% 실패
```
- 재고 보상 트랜잭션 추가 (외부 API 실패 시 복원)
- 실패한 건은 재고가 감소된 상태로 손실 발생

📊 이전 버전 테스트 결과
- 요청 1500개 중 604개 실패 (재고 404 + API 실패 500)
- 발급된 쿠폰은 1000보다 적다 (중간 손실)

#### 실패시 재고복원 로직의 추가
```kotlin
private val USER_COUPON_KEY = "rudals:user:coupon:"
private val INVENTORY_KEY = "rudals:coupon:available"

fun issueCoupon(userId: Long): Coupon? {
    val userKey = "$USER_COUPON_KEY$userId"

    // Already check : 캐시확인후 존재하면 그대로 반환
    val get = redisTemplate.opsForValue().get(userKey)
    if( get != null ) return couponRepository.findByCode(get)
        ?: error("쿠폰 정보가 Redis에는 있는데 DB에는 없습니다.")

    // Valid check : 재고체크(LPOP)
    val couponCode = redisTemplate.opsForList().leftPop(INVENTORY_KEY)
        ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰 재고가 모두 소진되었습니다.") // invalid 404

    // 재고 발급후 외부 api호출 ( 에러율 10% )
    try{
        MockConvenienceStoreUtil.sendCouponCodeToStore(couponCode)
    } catch (e: IllegalStateException) {
        // 실패 시 쿠폰 재고로 되돌리기
        redisTemplate.opsForList().rightPush(INVENTORY_KEY, couponCode)

        // 유저에게는 500 에러 그대로 반환
        throw ResponseStatusException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "쿠폰발급 후처리에 실패했습니다."
        )
    }

    // 쿠폰발급 -> redis에 저장
    redisTemplate.opsForValue().set(userKey, couponCode)

    // 반환
    return couponRepository.findByCode(couponCode)
        ?: error("DB에서 해당 쿠폰을 찾을 수 없습니다.")
}
```

📊 JMeter(1500TPS) 테스트 결과
| 항목    | 값                 |
| ----- | ----------------- |
| 요청 1500 | 모두 성공 송신 |
| Error% | 33.33%(재고부족 500건) |
| 중복발급  | 없음   |
| Redis재고 | 정확히 1000장 소비, 재고손실없음  |

➡️ 결론 : 보상 트랜잭션 적용 후 재고 정합성 100% 확보

## 📌 회고
- Redis의 Atomic 연산(LPOP/RPUSH)은 DB락 없이도 안전한 재고관리를 가능하게 한다.
  - 하지만 외부 API 실패 같은 비원자적 작업 때문에 재고 손실이 발생할 수 있다.
  - 이 문제는 보상 트랜잭션(재고 복원)으로 해결할 수 있다.
- Redis 기반 재고관리는 고성능 서버에서 매우 적합하며, DB 부담을 획기적으로 줄인다.
