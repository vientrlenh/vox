package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

public record GetStreamTokenCommand(
    List<UUID> roomIds, 
    UUID examId, 
    List<String> streamTypes
) {


}
