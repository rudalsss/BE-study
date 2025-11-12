package com.example.coupon_service.service

import com.example.coupon_service.entity.CouponInventory
import com.example.coupon_service.entity.UserCoupon
import com.example.coupon_service.repository.CouponInventoryRepository
import com.example.coupon_service.repository.UserCouponRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.random.Random
import org.slf4j.LoggerFactory

@Service
open class CouponService(
    private val couponInventoryRepository: CouponInventoryRepository,
    private val userCouponRepository: UserCouponRepository
){
    private val logger = LoggerFactory.getLogger(CouponService::class.java)

    @Transactional
    open fun issueCoupon(userId: Long, inventoryId: Long): UserCoupon {

        logger.info("쿠폰 발급 요청: userId=$userId, inventoryId=$inventoryId")
        // 해당 유저의 쿠폰존재 확인 -> 있다면 반환
        val existingUserCoupon = userCouponRepository.findByUserId(userId)
        if (existingUserCoupon!= null) {
            logger.info("유저 ($userId)는 이미 쿠폰을 발급받았습니다.")
            return existingUserCoupon
        }

        // 쿠폰 인벤토리 확인
        val couponInventory = couponInventoryRepository.findForUpdate(inventoryId)
            .orElseThrow{
                logger.info("해당 쿠폰 인벤토리($inventoryId)를 찾을 수 없습니다.")
                throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 쿠폰 인벤토리를 찾을 수 없습니다.")
            }

        // 재고확인
        if( !validateCoupounInventory(couponInventory) ){
            logger.error("쿠폰 재고가 모두 소진되었습니다. inventoryId=$inventoryId")
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰재고가 모두 소진되었습니다.")
        }

        // 쿠폰코드 생성
        val couponCode = generateValidCouponCode()
        // 쿠폰 발급
        val userCoupon = UserCoupon(
            userId = userId,
            inventoryId = inventoryId,
            couponCode = couponCode
        )

        // 쿠폰 재고증가
        couponInventory.assignedCouponCount++
        couponInventoryRepository.save(couponInventory)

        // 유저 쿠폰 저장
        logger.info("유저 $userId 의 쿠폰이 성공적으로 발급되었습니다.")
        return userCouponRepository.save(userCoupon)
    }

    private fun validateCoupounInventory(couponInventory: CouponInventory): Boolean{
        return couponInventory.totalCouponCount > couponInventory.assignedCouponCount
    }

    private fun generateValidCouponCode(): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        var couponCode: String
        do{
            couponCode = (1..10)
                .map { characters[Random.nextInt(characters.length)] }
                .joinToString("")
        } while (userCouponRepository.findByCouponCode(couponCode).isPresent)  // 중복된 코드가 있으면 다시 생성
        return couponCode
    }

    fun findUserCouponByUserId(userId: Long):UserCoupon {
        return userCouponRepository.findById(userId)
            .orElseThrow{ IllegalArgumentException("유저의 쿠폰을 찾을 수 없습니다.") }
    }
}