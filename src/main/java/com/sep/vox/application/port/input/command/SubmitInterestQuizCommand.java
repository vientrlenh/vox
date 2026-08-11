package com.sep.vox.application.port.input.command;

import java.util.List;

import com.sep.vox.domain.model.personalization.QuizAnswer;

public record SubmitInterestQuizCommand(List<QuizAnswer> answers) {
}
