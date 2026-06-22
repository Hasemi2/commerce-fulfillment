package com.shopflow.inventory.order.presentation;

import com.shopflow.inventory.common.response.ErrorResponse;
import com.shopflow.inventory.order.application.OrderCreateCommand;
import com.shopflow.inventory.order.application.OrderQueryService;
import com.shopflow.inventory.order.application.OrderService;
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
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 API")
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

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
            description = "재고 미등록 또는 재고 부족",
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
        Order order = orderService.createOrder(command);
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
}
