package com.sep.vox.interfaces.graphql.input.learnerprofile;

import java.util.List;

public record SubmitInterestQuizInput(List<InterestQuizAnswerInput> answers) {
}
