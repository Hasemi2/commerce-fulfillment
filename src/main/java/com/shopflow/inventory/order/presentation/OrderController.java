package com.shopflow.inventory.order.presentation;

import com.shopflow.inventory.common.response.ErrorResponse;
import com.shopflow.inventory.order.application.*;
import com.shopflow.inventory.order.domain.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 API")
public class OrderController {

    private final OrderLockFacade orderLockFacade;
    private final OrderQueryService orderQueryService;
    private final OrderCancellationService orderCancellationService;
    private final OrderPaymentService orderPaymentService;

    @PostMapping
    @Operation(summary = "주문 생성")
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "주문 생성 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 요청",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_REQUEST","message":"주문 상품은 하나 이상이어야 합니다."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "404",
            description = "상품 없음",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"PRODUCT_NOT_FOUND","message":"Product was not found."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "재고 미등록, 재고 부족 또는 재고 락 획득 실패",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = {
                    @ExampleObject(
                        name = "inventoryNotRegistered",
                        value = """
                            {"code":"INVENTORY_NOT_REGISTERED","message":"Inventory is not registered for this product."}
                            """
                    ),
                    @ExampleObject(
                        name = "notEnoughStock",
                        value = """
                            {"code":"NOT_ENOUGH_STOCK","message":"Not enough stock is available."}
                            """
                    ),
                    @ExampleObject(
                        name = "inventoryLockFailed",
                        value = """
                            {"code":"INVENTORY_LOCK_FAILED","message":"Failed to acquire inventory lock."}
                            """
                    )
                }
            )
        )
    })
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody OrderCreateRequest request
    ) {
        OrderCreateCommand command = new OrderCreateCommand(
            request.memberId(),
            request.items().stream()
                .map(item -> new OrderCreateCommand.OrderItemCommand(
                    item.productId(),
                    item.quantity()
                ))
                .toList()
        );
        Order order = orderLockFacade.createOrder(command);
        OrderResponse response = OrderResponse.from(order);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id())).body(response);
    }

    @GetMapping
    @Operation(summary = "회원 주문 목록 기간 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 목록 조회 성공",
            content = @Content(
                array = @ArraySchema(schema = @Schema(implementation = OrderSummaryResponse.class))
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "잘못된 회원 ID 또는 날짜 범위",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_ORDER_DATE_RANGE","message":"Order date range is invalid."}
                    """)
            )
        )
    })
    public List<OrderSummaryResponse> getOrders(
        @Parameter(description = "회원 ID", example = "10")
        @RequestParam Long memberId,
        @Parameter(description = "조회 시작일", example = "2026-06-01")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
        @Parameter(description = "조회 종료일", example = "2026-06-22")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return orderQueryService.getOrders(memberId, fromDate, toDate).stream()
            .map(OrderSummaryResponse::from)
            .toList();
    }

    @GetMapping("/{orderNo}")
    @Operation(summary = "주문번호로 주문 상세 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 상세 조회 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 없음",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"ORDER_NOT_FOUND","message":"Order was not found."}
                    """)
            )
        )
    })
    public OrderResponse getOrder(
        @Parameter(
            description = "주문 번호",
            example = "ORD-550e8400e29b41d4a716446655440000"
        )
        @PathVariable String orderNo
    ) {
        return OrderResponse.from(orderQueryService.getOrder(orderNo));
    }

    @PostMapping("/{orderNo}/pay")
    @Operation(summary = "Complete order payment")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "Order payment completed",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "Order was not found",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"ORDER_NOT_FOUND","message":"Order was not found."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "Order status transition is not allowed",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_ORDER_STATUS_TRANSITION","message":"Order status transition is not allowed."}
                    """)
            )
        )
    })
    public OrderResponse payOrder(
        @Parameter(
            description = "Order number",
            example = "ORD-550e8400e29b41d4a716446655440000"
        )
        @PathVariable String orderNo
    ) {
        return OrderResponse.from(orderPaymentService.payOrder(orderNo));
    }

    @PostMapping("/{orderNo}/cancel")
    @Operation(summary = "주문 취소")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "주문 취소 성공",
            content = @Content(schema = @Schema(implementation = OrderResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "주문 없음",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"ORDER_NOT_FOUND","message":"Order was not found."}
                    """)
            )
        ),
        @ApiResponse(
            responseCode = "409",
            description = "취소할 수 없는 주문 상태",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_ORDER_STATUS_TRANSITION","message":"Order status transition is not allowed."}
                    """)
            )
        )
    })
    public OrderResponse cancelOrder(
        @Parameter(
            description = "주문 번호",
            example = "ORD-550e8400e29b41d4a716446655440000"
        )
        @PathVariable String orderNo
    ) {
        return OrderResponse.from(orderCancellationService.cancelOrder(orderNo));
    }
}
