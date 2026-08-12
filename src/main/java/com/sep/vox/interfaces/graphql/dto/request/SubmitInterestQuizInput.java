package com.sep.vox.interfaces.graphql.dto.request;

import java.util.List;

public record SubmitInterestQuizInput(List<InterestQuizAnswerInput> answers) {
}
