# 🚀 쿠폰발급시스템 : 동시성 관리 (DB Pessimistic Lock vs Optimistic Lock)

## 🎯 학습 목표
- 하나의 리소스(쿠폰)에서 다수의 동시 요청이 발생할 때 정합성 깨짐(Reace Condition) 문제를 이해한다.
- Spring JPA의 비관적 락(Pessimistic Write)과 낙관적 락(Optimistic Lock + Versioning) 을 실험을 통해 비교한다.
- JMeter로 동시성 테스트를 수행하고, 처리량/실패율/정합성을 분석한다.

## ✅ 개발환경
기술 스택
- Kotlin + Spring Boot
- Spring Data JPA / Hibernate
- MySQL
- Apache JMeter 5.6

테스트 조건 (JMeter)
| 항목             | 값                                           |
| -------------- | ------------------------------------------- |
| Threads        | 200                                         |
| Loop Count     | 1                                           |
| Ramp-Up Period | 1 sec                                       |
| 요청 경로          | `POST /api/v1/coupons/${__threadNum}/issue` |

## ✅ 테스트 대상 도메인 구조
📌 CouponInventory (쿠폰 재고 엔티티)
- totalCouponCount : 총 발급 가능 쿠폰(100장)
- assignedCouponCount : 현재까지 발급한 쿠폰
- @Version : Optimistic Lock에서 사용

📌 UserCoupon (유저 쿠폰 엔티티)
- 사용자가 발급받은 쿠폰 정보 저장

---

## 1️⃣ 기본 구현 — 락 없음 (Race Condition 발생)
동시 요청 시 시나리오
- 동시에 200명의 사용자가 발급 요청
- select 후 update 사이에 경쟁 조건 존재
- 결국 100장을 넘겨 초과 발급 발생

핵심 코드
```kotlin
val couponInventory = couponInventoryRepository.findById(inventoryId).orElseThrow()
couponInventory.assignedCouponCount++
couponInventoryRepository.save(couponInventory)
```

📊 테스트 결과
| 항목    | 값                 |
| ----- | ----------------- |
| 처리량   | **199.8 req/sec** |
| 정상 처리 | **147 건**         |
| 실패율   | **29%** (재고 초과)   |
| 문제    | 재고 100장 초과 발급 발생  |

👉 정합성 완전히 깨짐 — 테스트 실패

<br>

## 2️⃣ Pessimistic Lock — PESSIMISTIC_WRITE 적용
JPA 설정추가
```kotlin
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT i FROM CouponInventory i WHERE i.id = :couponId")
fun findForUpdate(@Param("couponId") couponId: Long): Optional<CouponInventory>
```

동작 방식
- 쿠폰 재고 row에 DB 수준의 쓰기 락을 걸어, 다른 트랜잭션이 해당 row를 건드릴 수 없게 막음
- 200명 요청이 모두 같은 row를 바라보기 때문에 실질적으로 **순차 처리**됨

📊 테스트 결과
| 항목    | 값                     |
| ----- | --------------------- |
| 처리량   | **200.8 req/sec**     |
| 정상 처리 | **100 건**             |
| 실패율   | **50%** (재고 부족)       |
| 특징    | 정합성 100% 확보, 성능 다소 저하 |

👉 정합성 완전 확보 – 초과 발급 없음, 트랜잭션 대기시간이 증가하여 처리율이 소폭 하락

<br>

## 3️⃣ Optimistic Lock — @Version 기반 충돌 감지
CouponInventory version 필드 추가
```kotlin
@Version
@Column(nullable = false)
var version: Long = 0
```
낙관적 락 동작 방식
- 트랜잭션 종료 시 update 쿼리에 where version = ? 조건 포함
- 버전이 다르면 OptimisticLockException 발생 → rollback
- 충돌 시 빠르게 실패하는 구조

📊 기본 낙관적 락 결과
| 항목    | 값                   |
| ----- | ------------------- |
| 처리량   | 200.2 req/sec       |
| 정상 처리 | 100 건               |
| 실패율   | 50% (재고 부족 + 버전 충돌) |
| 특징    | 정합성 100%, 성능 양호     |

<br>

## 4️⃣ Optimistic Lock 개선 — 빠른 실패 전략 적용
핵심 아이디어
- 재고 증가 → 즉시 flush (버전 갱신)
- 그 후에 쿠폰 생성 / 저장 로직 수행
- 충돌 시 즉각 실패 → 불필요한 작업 최소화

개선된 코드
```kotlin
couponInventory.assignedCouponCount++
couponInventoryRepository.flush()   // version 즉시 반영 → 충돌 빠른 감지
```

📊 테스트 결과
| 항목    | 값                     |
| ----- | --------------------- |
| 처리량   | **199.4 req/sec**     |
| 정상 처리 | **100 건**             |
| 실패율   | **50%**               |
| 특징    | 성능 가장 바람직함(락 + 빠른 실패) |

<br>

## 🧩 결과 비교 (Summary)
#### 🔸 처리량(Troughput) 비교
| 방식            | Throughput | 특징             |
| ------------- | ---------- | -------------- |
| 락 없음          | 199.8      | 처리량 높지만 정합성 깨짐 |
| Pessimistic   | 200.8      | 순차 처리로 안정적     |
| Optimistic    | 200.2      | 충돌 발생하지만 즉시 실패 |
| Optimistic 개선 | 199.4      | 불필요 로직 최소화     |
#### 🔸 정합성 비교
| 방식            | 재고 100장 유지 | 초과 발급 발생 |
| ------------- | ---------- | -------- |
| 락 없음          | ❌          | ⭕️       |
| Pessimistic   | ⭕️          | ❌        |
| Optimistic    | ⭕️         | ❌        |
| Optimistic 개선 | ⭕️          | ❌        |

<br>

## 회고
- Race Condition은 읽기→검증→쓰기 시나리오에서 쉽게 발생한다.
- JPA/Hibernate의 Locking 메커니즘(Pessimistic/Optimistic)이 어떻게 동작하는지 실험으로 체감할 수 있었다.
- Optimistic Lock에서 “버전 충돌 즉시 실패” 전략이 성능 최적화에 핵심이었다.
- 단일 리소스(재고 1건) 병목 상황에서 트랜잭션 설계의 중요성을 다시 확인했다.
