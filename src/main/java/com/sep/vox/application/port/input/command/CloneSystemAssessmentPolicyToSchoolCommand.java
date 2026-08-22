package com.sep.vox.application.port.input.command;

import java.time.Instant;
import java.util.UUID;

import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;

/**
 * Trường sao một chính sách mẫu đã ban hành của hệ thống về làm chính sách riêng.
 *
 * @param schoolId          trường nhận bản sao.
 * @param sourcePolicyId    chính sách mẫu (schoolId IS NULL, PUBLISHED).
 * @param rubricCode        mã bộ tiêu chí do TRƯỜNG đặt cho bản sao rubric đi kèm. Sao chính sách
 *                          bắt buộc kéo theo sao rubric vì bộ tiêu chí của bản mẫu thuộc sở hữu
 *                          SYSTEM, mà chính sách của trường chỉ gắn được vào rubric của chính
 *                          trường đó -- cùng lý do khiến CloneSystemRubricToSchoolUseCase tồn tại.
 *                          (Không phải vì ràng buộc 1-1: V44 đã cho nhiều chính sách dùng chung một
 *                          phiên bản rubric, nên các lớp tiếp theo gắn thẳng vào bản sao này.)
 * @param rubricName        tên bộ tiêu chí của bản sao.
 * @param rubricDescription mô tả bản sao; bỏ trống thì giữ theo bản mẫu.
 * @param totalScoreMethod  cách tính điểm cho bản sao rubric; null = giữ như bản mẫu.
 * @param gradeLevelId Khối áp dụng khi bản mẫu KHÔNG khai khối. Bản mẫu đã khai khối thì bản
 *                          sao kế thừa đúng khối đó và tham số này phải để trống.
 * @param schoolGradeId     Niên khóa áp dụng, thay cho Khối (chỉ khi bản mẫu không khai khối).
 * @param schoolClassId     Lớp áp dụng, thay cho Khối (chỉ khi bản mẫu không khai khối).
 * @param effectiveFrom     ngày bắt đầu hiệu lực do trường quyết định.
 * @param effectiveTo       ngày kết thúc hiệu lực; null = không giới hạn.
 */
public record CloneSystemAssessmentPolicyToSchoolCommand(
        UUID schoolId,
        UUID sourcePolicyId,
        String rubricCode,
        String rubricName,
        String rubricDescription,
        RubricTotalScoreMethod totalScoreMethod,
        UUID gradeLevelId,
        UUID schoolGradeId,
        UUID schoolClassId,
        Instant effectiveFrom,
        Instant effectiveTo) {}
