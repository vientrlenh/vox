package com.sep.vox.application.port.input.query;

import java.util.List;
import java.util.UUID;

public record ViewPracticeTopicOffersQuery(List<UUID> excludeTopicIds, int round, String bucket) {
}
