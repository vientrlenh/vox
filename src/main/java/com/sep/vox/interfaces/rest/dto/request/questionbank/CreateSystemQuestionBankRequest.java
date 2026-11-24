package com.sep.vox.interfaces.rest.dto.request.questionbank;

import com.sep.vox.application.port.input.command.questionbank.CreateSystemQuestionBankCommand;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateSystemQuestionBankRequest(

    @NotNull(message = "ID ngôn ngữ không được để trống")
    UUID languageId,

    @NotBlank(message = "Mã ngân hàng câu hỏi không được để trống")
    @Size(max = 100, message = "Mã ngân hàng câu hỏi không được vượt quá 100 ký tự")
    String code, 

    @NotBlank(message = "Tên ngân hàng câu hỏi không được để trống")
    @Size(max = 255, message = "Tên ngân hàng câu hỏi không được vượt quá 255 ký tự")
    String name, 

    @Size(max = 2048, message = "Mô tả ngân hàng câu hỏi không được vượt quá 2048 ký tự")
    String description
) {

    public static CreateSystemQuestionBankCommand toCommand(CreateSystemQuestionBankRequest request) {
        return new CreateSystemQuestionBankCommand(
            request.languageId(), 
            request.code(), 
            request.name(), 
            request.description()
        );
    }
}
