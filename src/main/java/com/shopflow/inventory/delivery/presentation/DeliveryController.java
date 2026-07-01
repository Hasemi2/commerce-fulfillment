package com.shopflow.inventory.delivery.presentation;

import com.shopflow.inventory.common.response.ErrorResponse;
import com.shopflow.inventory.delivery.application.DeliveryQueryService;
import com.shopflow.inventory.delivery.application.DeliveryRequestService;
import com.shopflow.inventory.delivery.domain.DeliveryStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/deliveries")
@Tag(name = "Delivery", description = "배송 요청 API")
public class DeliveryController {

    private final DeliveryQueryService deliveryQueryService;
    private final DeliveryRequestService deliveryRequestService;

    @GetMapping
    @Operation(summary = "배송 요청 목록 조회")
    @ApiResponse(
        responseCode = "200",
        description = "배송 요청 목록 조회 성공",
        content = @Content(
            array = @ArraySchema(schema = @Schema(implementation = DeliveryRequestResponse.class))
        )
    )
    public List<DeliveryRequestResponse> getDeliveryRequests(
        @Parameter(description = "배송 요청 상태", example = "FAILED")
        @RequestParam(required = false) DeliveryStatus status
    ) {
        return deliveryQueryService.getDeliveryRequests(status)
                .stream()
                .map(DeliveryRequestResponse::from)
                .toList();
    }

    @GetMapping("/{deliveryRequestId}")
    @Operation(summary = "배송 요청 단건 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "배송 요청 조회 성공",
            content = @Content(schema = @Schema(implementation = DeliveryRequestResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "배송 요청 없음",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"DELIVERY_REQUEST_NOT_FOUND","message":"Delivery request was not found."}
                    """)
            )
        )
    })
    public DeliveryRequestResponse getDeliveryRequest(
        @Parameter(description = "배송 요청 ID", example = "1")
        @PathVariable Long deliveryRequestId
    ) {
        return DeliveryRequestResponse.from(
            deliveryQueryService.getDeliveryRequest(deliveryRequestId)
        );
    }

    @GetMapping("/orders/{orderNo}")
    @Operation(summary = "주문번호로 배송 요청 조회")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "배송 요청 조회 성공",
            content = @Content(schema = @Schema(implementation = DeliveryRequestResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "배송 요청 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
    })
    public DeliveryRequestResponse getDeliveryRequestByOrderNo(
        @Parameter(
            description = "주문 번호",
            example = "ORD-550e8400e29b41d4a716446655440000"
        )
        @PathVariable String orderNo
    ) {
        return DeliveryRequestResponse.from(
            deliveryQueryService.getDeliveryRequestByOrderNo(orderNo)
        );
    }

    @PostMapping("/{deliveryRequestId}/retry")
    @Operation(summary = "실패 배송 요청 재시도")
    @ApiResponses({
        @ApiResponse(
            responseCode = "200",
            description = "배송 요청 재시도 완료",
            content = @Content(schema = @Schema(implementation = DeliveryRequestResponse.class))
        ),
        @ApiResponse(
            responseCode = "404",
            description = "배송 요청 없음",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
            responseCode = "409",
            description = "재시도할 수 없는 배송 상태",
            content = @Content(
                schema = @Schema(implementation = ErrorResponse.class),
                examples = @ExampleObject(value = """
                    {"code":"INVALID_DELIVERY_STATUS_TRANSITION","message":"Delivery status transition is not allowed."}
                    """)
            )
        )
    })
    public DeliveryRequestResponse retryDelivery(
        @Parameter(description = "배송 요청 ID", example = "1")
        @PathVariable Long deliveryRequestId
    ) {
        return DeliveryRequestResponse.from(
            deliveryRequestService.retryDelivery(deliveryRequestId)
        );
    }
}
