package com.sep.vox.interfaces.rest.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateQuestionAssetRequest(
    @Size(max = 255, message = "TiÃªu Ä‘á» tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 255 kÃ½ tá»±")
    String title,

    @Min(value = 0, message = "Thá»i lÆ°á»£ng tÃ i nguyÃªn pháº£i lá»›n hÆ¡n hoáº·c báº±ng 0")
    Integer durationSeconds,

    @Size(max = 255, message = "VÄƒn báº£n thay tháº¿ khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 255 kÃ½ tá»±")
    String altText,

    @NotBlank(message = "Loáº¡i tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Pattern(
        regexp = "AUDIO|IMAGE|VIDEO|TEXT_PASSAGE",
        message = "Loáº¡i tÃ i nguyÃªn khÃ´ng há»£p lá»‡"
    )
    String type,

    @Size(max = 4096, message = "URL tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 4096 kÃ½ tá»±")
    String url,

    String transcript,

    @Size(max = 2048, message = "MÃ´ táº£ tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c vÆ°á»£t quÃ¡ 2048 kÃ½ tá»±")
    String description,

    @NotNull(message = "Thá»© tá»± tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ trá»‘ng")
    @Min(value = 0, message = "Thá»© tá»± tÃ i nguyÃªn khÃ´ng Ä‘Æ°á»£c nhá» hÆ¡n 0")
    Integer order
) {
}
