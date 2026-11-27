package com.sep.vox.application.port.input.usecase.registration;

import com.sep.vox.domain.model.registerform.RegisterForm;
import java.time.Instant;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.RegisterFormRejectedPayloadV1;
import com.sep.vox.application.exception.ResourceNotFoundException;
import com.sep.vox.application.port.input.command.registration.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;

@Service
@RequiredArgsConstructor
public class RejectRegisterFormUseCase implements IUseCase<RejectRegisterFormCommand, Void>{

    private final RegisterFormRepository registerFormRepository;
    private final OutboxRepository outboxRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    @Override
    @Transactional
    public Void execute(RejectRegisterFormCommand input) {
        RejectRegisterFormCommand command = normalize(input);
        Instant now = Instant.now();
        UUID currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        RegisterForm registerForm = registerFormRepository.findById(command.registerFormId())
            .orElseThrow(() -> new ResourceNotFoundException("Register form not found"));

        int updatedRows = registerFormRepository.updateRejectedRegisterForm(command.registerFormId(), currentUserId, command.reason());
        if (updatedRows == 0) {
            throw new IllegalArgumentException("The register form is not pending or does not exist");
        }
        
        outboxRepository.save(toRejectedOutboxEvent(registerForm, command.reason(), now));

        return null;
    }

    private Outbox toRejectedOutboxEvent(RegisterForm registerForm, String reason, Instant now) {
        RegisterFormRejectedPayloadV1 event = new RegisterFormRejectedPayloadV1(
                registerForm.getContactEmail().value(), reason);
        String payload = jsonSerializationPort.toJson(event);

        return Outbox.create(
            AggregateTypeConstant.REGISTER_FORM,
            registerForm.getId(),
            EventTypeConstant.REGISTER_FORM_REJECTED,
            payload,
            now
        );
    }
    
    private RejectRegisterFormCommand normalize(RejectRegisterFormCommand input) {
        return new RejectRegisterFormCommand(
            input.registerFormId(),
            StringNormalization.trimAndCollapseSpaces(input.reason())
        );
    }

}
