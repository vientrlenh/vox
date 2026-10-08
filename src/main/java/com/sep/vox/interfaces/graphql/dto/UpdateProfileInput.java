package com.sep.vox.interfaces.graphql.dto;

import org.springframework.graphql.data.ArgumentValue;

import com.sep.vox.application.port.input.command.UpdateProfileCommand;
import com.sep.vox.interfaces.shared.GraphQLInputChecker;

public record UpdateProfileInput(
    ArgumentValue<String> fullName, 
    ArgumentValue<String> phone, 
    ArgumentValue<String> address, 
    ArgumentValue<String> avatarUrl
) {
    
    public static UpdateProfileCommand toCommand(UpdateProfileInput input) {
        boolean fullNameProvided = true, phoneProvided = true, addressProvided = true, avatarUrlProvided = true;
        String fullName = "", phone = "", address = "", avatarUrl = "";
        if (!GraphQLInputChecker.checkStringIfBlank(input.fullName)) {
            fullNameProvided = false;
        } else {
            fullName = input.fullName.value();
        }
        if (!GraphQLInputChecker.checkStringIfBlank(input.phone)) {
            phoneProvided = false;
        } else {
            phone = input.phone.value();
        }
        if (!GraphQLInputChecker.checkStringIfBlank(input.address)) {
            addressProvided = false;
        } else {
            address = input.address.value();
        }
        if (!GraphQLInputChecker.checkStringIfNull(input.avatarUrl)) {
            avatarUrlProvided = false;   
        } else {
            avatarUrl = input.avatarUrl.value();
        }
        return new UpdateProfileCommand(
            fullName, 
            fullNameProvided, 
            phone, 
            phoneProvided, 
            address, 
            addressProvided, 
            avatarUrl, 
            avatarUrlProvided
        );
    }


}
