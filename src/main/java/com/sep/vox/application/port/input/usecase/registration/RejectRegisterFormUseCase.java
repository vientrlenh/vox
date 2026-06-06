package com.sep.vox.application.port.input.usecase.registration;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.application.event.RegisterFormRejectedEvent;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.port.input.command.RejectRegisterFormCommand;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.EventPublisherPort;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.repository.RegisterFormRepository;

@Service
public class RejectRegisterFormUseCase implements IUseCase<RejectRegisterFormCommand, Void>{

    private final RegisterFormRepository registerFormRepository;
    private final UserContextPort userContextPort;
    private final EventPublisherPort eventPublisherPort;

    public RejectRegisterFormUseCase(RegisterFormRepository registerFormRepository, UserContextPort userContextPort, EventPublisherPort eventPublisherPort) {
        this.registerFormRepository = registerFormRepository;
        this.userContextPort = userContextPort;
        this.eventPublisherPort = eventPublisherPort;
    }

    @Override
    @Transactional
    public Void execute(RejectRegisterFormCommand input) {
        var command = normalize(input);
        var now = OffsetDateTime.now();
        var currentUserId = userContextPort.getCurrentAuthenticatedUserId();

        var registerForm = registerFormRepository.findById(command.registerFormId())
            .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn đăng ký"));

        var updatedRows = registerFormRepository.updateRejectedRegisterForm(command.registerFormId(), currentUserId, command.reason(), now);
        if (updatedRows == 0) {
            throw new IllegalStateException("Đơn đăng ký không ở trạng thái chờ hoặc không tồn tại");
        }

        eventPublisherPort.publish(new RegisterFormRejectedEvent(
            registerForm.getContactEmail().value(),
            command.reason()
        ));
        
        return null;
    }
    
    private RejectRegisterFormCommand normalize(RejectRegisterFormCommand input) {
        return new RejectRegisterFormCommand(
            input.registerFormId(),
            StringNormalization.trimAndCollapseSpaces(input.reason())
        );
    }
}
