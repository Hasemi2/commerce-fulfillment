package com.shopflow.inventory.common.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "API 오류 응답")
public record ErrorResponse(
    @Schema(description = "오류 코드", example = "INVALID_REQUEST")
    String code,

    @Schema(description = "오류 메시지", example = "요청 값이 올바르지 않습니다.")
    String message
) {
}
