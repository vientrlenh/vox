package com.sep.vox.application.port.input.usecase.registration;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.RegisterFormRejectedPayloadV1;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.JsonSerializationPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.common.AggregateTypeConstant;
import com.sep.vox.domain.common.EventTypeConstant;
import com.sep.vox.domain.model.outbox.Outbox;
import com.sep.vox.domain.repository.OutboxRepository;
import com.sep.vox.domain.repository.RegisterFormRepository;

@Service
public class RejectRegisterFormUseCase implements IUseCase<RejectRegisterFormCommand, Void>{

    private final RegisterFormRepository registerFormRepository;
    private final OutboxRepository outboxRepository;
    private final UserContextPort userContextPort;
    private final JsonSerializationPort jsonSerializationPort;

    public RejectRegisterFormUseCase(RegisterFormRepository registerFormRepository, 
        OutboxRepository outboxRepository,  
        UserContextPort userContextPort, JsonSerializationPort jsonSerializationPort) {
        this.registerFormRepository = registerFormRepository; 
        this.outboxRepository = outboxRepository;
        this.userContextPort = userContextPort;
        this.jsonSerializationPort = jsonSerializationPort;
    }

    @Override
    @Transactional
    public Void execute(RejectRegisterFormCommand input) {
        var command = normalize(input);
        var now = Instant.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var registerForm = registerFormRepository.findById(command.registerFormId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đăng ký"));

        var updatedRows = registerFormRepository.updateRejectedRegisterForm(command.registerFormId(), currentUserId, command.reason(), now);
        if (updatedRows == 0) {
            throw new IllegalStateException("Đơn đăng ký không ở trạng thái chờ hoặc không tồn tại");
        }
        
        var event = new RegisterFormRejectedPayloadV1(
                registerForm.getContactEmail().value(), command.reason());
        var payload = jsonSerializationPort.toJson(event);

        var outboxEvent = Outbox.create(
            AggregateTypeConstant.REGISTER_FORM, 
            registerForm.getId(), 
            EventTypeConstant.REGISTER_FORM_REJECTED, 
            payload, 
            now
        );

        outboxRepository.save(outboxEvent);
        
        return null;
    }
    
    private RejectRegisterFormCommand normalize(RejectRegisterFormCommand input) {
        return new RejectRegisterFormCommand(
            input.registerFormId(),
            StringNormalization.trimAndCollapseSpaces(input.reason())
        );
    }

}
