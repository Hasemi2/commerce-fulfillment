package com.shopflow.inventory.order.application;

import com.shopflow.inventory.common.exception.BusinessException;
import com.shopflow.inventory.common.exception.ErrorCode;
import com.shopflow.inventory.order.domain.Order;
import com.shopflow.inventory.order.infrastructure.OrderRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class OrderQueryService {

    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long memberId, LocalDate fromDate, LocalDate toDate) {
        validateMemberId(memberId);
        validateDateRange(fromDate, toDate);

        LocalDateTime fromDateTime = fromDate.atStartOfDay();
        LocalDateTime toDateTimeExclusive = toDate.plusDays(1).atStartOfDay();
        return orderRepository
            .findAllByMemberIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                memberId,
                fromDateTime,
                toDateTimeExclusive
            );
    }

    @Transactional(readOnly = true)
    public Order getOrder(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_NO);
        }
        return orderRepository.findByOrderNo(orderNo)
            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void validateMemberId(Long memberId) {
        if (memberId == null || memberId <= 0) {
            throw new BusinessException(ErrorCode.INVALID_MEMBER_ID);
        }
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_DATE_RANGE);
        }
    }
}
