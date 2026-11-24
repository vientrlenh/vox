package com.sep.vox.interfaces.graphql.input.user;

import java.time.LocalDate;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.user.UpdateProfileCommand;

public record UpdateProfileInput(
        ArgumentValue<String> fullName,
        ArgumentValue<String> phone,
        ArgumentValue<String> address,
        ArgumentValue<LocalDate> dateOfBirth,
        ArgumentValue<String> avatarUrl) {

    public static UpdateProfileCommand toCommand(UpdateProfileInput input) {
        return new UpdateProfileCommand(
                input.fullName().value(), !input.fullName().isOmitted(),
                input.phone().value(), !input.phone().isOmitted(),
                input.address().value(), !input.address().isOmitted(),
                input.dateOfBirth().value(), !input.dateOfBirth().isOmitted(),
                input.avatarUrl().value(), !input.avatarUrl().isOmitted());
    }
}
