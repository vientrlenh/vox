package com.sep.vox.application.port.input.command;

import java.util.List;
import java.util.UUID;

/**
 * @param targetFrameworkBandId bậc học sinh CHỌN cho phiên này. Trước đây hệ thống tự suy ra
 *     bậc rồi ép học sinh luyện theo; giờ đây là lựa chọn của người học, và bậc chỉ còn nghĩa
 *     "độ khó tôi muốn hôm nay". Bắt buộc -- không có mặc định nào đúng thay cho học sinh.
 */
public record BuildPracticePaperCommand(
    UUID topicId,
    UUID targetFrameworkBandId,
    String origin,
    String fromSubAttribute,
    List<UUID> offeredTopicIds,
    List<UUID> previousOfferedTopicIds
) {
}
