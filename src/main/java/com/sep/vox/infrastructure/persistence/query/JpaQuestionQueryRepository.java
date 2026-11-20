package com.sep.vox.infrastructure.persistence.query;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.application.query.dto.QuestionStatusCountInfo;
import com.sep.vox.application.query.repository.QuestionQueryRepository;
import com.sep.vox.domain.model.question.QuestionStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class JpaQuestionQueryRepository implements QuestionQueryRepository {

    /**
     * Khối điều kiện dưới đây là BẢN SAO NGUYÊN VĂN phần WHERE của
     * {@code SpringDataQuestionRepository.findAccessible}, chỉ bỏ đúng hai thứ: bộ lọc
     * {@code :status} (vì status là chiều đang nhóm) và {@code ORDER BY} (vô nghĩa khi
     * gộp). Sửa một bên mà quên bên kia là con số trên biểu đồ lệch khỏi danh sách người
     * dùng bấm vào -- kiểu sai không ai báo, chỉ âm thầm cho ra tổng khác.
     *
     * <p>Vì vậy tuyệt đối không "tối ưu" riêng khối này cho gọn.
     */
    private static final String ACCESSIBLE_QUESTIONS_JPQL = """
        SELECT q.status, COUNT(q)
        FROM QuestionJpaEntity q
        JOIN QuestionTopicJpaEntity qt ON qt.id = q.questionTopicId
        JOIN QuestionBankJpaEntity qb ON qb.id = q.questionBankId
        WHERE (:questionBankId IS NULL OR q.questionBankId = :questionBankId)
          AND (:questionTopicId IS NULL OR q.questionTopicId = :questionTopicId)
          AND (:topicNamePattern IS NULL OR LOWER(qt.name) LIKE :topicNamePattern)
          AND (:type IS NULL OR q.type = :type)
          AND (:sharing IS NULL OR q.sharing = :sharing)
          AND (
                :scope IS NULL
                OR :scope = 'ALL'
                OR (:scope = 'MINE' AND q.createdBy = :currentUserId)
                OR (
                    :scope = 'COLLABORATING'
                    AND EXISTS (
                        SELECT 1
                        FROM QuestionCollaboratorJpaEntity qc
                        WHERE qc.questionId = q.id
                          AND qc.userId = :currentUserId
                    )
                )
                OR (
                    :scope = 'REVIEWING'
                    AND EXISTS (
                        SELECT 1
                        FROM QuestionCollaboratorJpaEntity qc
                        WHERE qc.questionId = q.id
                          AND qc.userId = :currentUserId
                          AND qc.permission = 'CAN_EDIT'
                    )
                )
              )
          AND (
                :keywordPattern IS NULL
                OR LOWER(q.code) LIKE :keywordPattern
                OR LOWER(q.questionText) LIKE :keywordPattern
              )
          AND (
                (:systemAdmin = true AND qb.ownerType = 'SYSTEM')
                OR EXISTS (
                    SELECT 1
                    FROM ExamPaperItemJpaEntity epi
                    JOIN ExamPaperJpaEntity ep ON ep.id = epi.paperId
                    JOIN ExamMemberJpaEntity em ON em.examId = ep.examId
                    WHERE epi.questionId = q.id
                      AND em.userId = :currentUserId
                )
                OR EXISTS (
                    SELECT 1
                    FROM ExamBlueprintSlotJpaEntity bs
                    JOIN ExamBlueprintVersionJpaEntity bv ON bv.id = bs.blueprintVersionId
                    JOIN ExamBlueprintJpaEntity b ON b.id = bv.blueprintId
                    WHERE bs.fixedQuestionId = q.id
                      AND (:systemAdmin = true OR b.schoolId = :currentSchoolId)
                )
                OR (
                    (
                        :systemAdmin = true
                        AND qb.ownerType = 'SCHOOL'
                        AND qb.status = 'PUBLISHED'
                        AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        qb.ownerType = 'SYSTEM'
                        AND qb.status = 'PUBLISHED'
                        AND qt.status = 'PUBLISHED'
                        AND q.status = 'PUBLISHED'
                        AND q.sharing = 'SCHOOL_SHARED'
                    )
                    OR (
                        :schoolAdmin = true
                        AND (
                            qb.schoolId = :currentSchoolId
                            OR (
                                qb.status = 'PUBLISHED'
                                AND qt.status = 'PUBLISHED'
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                    OR (
                        :schoolAdmin = false
                        AND (
                            q.createdBy = :currentUserId
                            OR EXISTS (
                                SELECT 1
                                FROM QuestionCollaboratorJpaEntity qc
                                WHERE qc.questionId = q.id
                                  AND qc.userId = :currentUserId
                            )
                            OR (
                                qb.schoolId = :currentSchoolId
                                AND qb.ownerType = 'SCHOOL'
                                AND qb.status = 'PUBLISHED'
                                AND qt.status = 'PUBLISHED'
                                AND q.status = 'PUBLISHED'
                                AND q.sharing = 'SCHOOL_SHARED'
                            )
                        )
                    )
                )
              )
        GROUP BY q.status
        """;

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<QuestionStatusCountInfo> countAccessibleByStatus(
            UUID currentUserId,
            UUID currentSchoolId,
            boolean systemAdmin,
            boolean schoolAdmin,
            UUID questionBankId,
            UUID questionTopicId,
            String topicNamePattern,
            String type,
            String sharing,
            String scope,
            String keywordPattern) {
        // Không dùng SELECT new ... được: cột status của QuestionJpaEntity là String (xem
        // @Column ở entity, ràng buộc bằng CHECK chứ không phải @Enumerated), nên constructor
        // nhận QuestionStatus sẽ không khớp. Đổi String -> enum tại đây là đúng chỗ: đây là
        // nơi duy nhất biết cách entity lưu trữ.
        var rows = em.createQuery(ACCESSIBLE_QUESTIONS_JPQL, Object[].class)
            .setParameter("currentUserId", currentUserId)
            .setParameter("currentSchoolId", currentSchoolId)
            .setParameter("systemAdmin", systemAdmin)
            .setParameter("schoolAdmin", schoolAdmin)
            .setParameter("questionBankId", questionBankId)
            .setParameter("questionTopicId", questionTopicId)
            .setParameter("topicNamePattern", topicNamePattern)
            .setParameter("type", type)
            .setParameter("sharing", sharing)
            .setParameter("scope", scope)
            .setParameter("keywordPattern", keywordPattern)
            .getResultList();

        return rows.stream()
            .map(row -> new QuestionStatusCountInfo(
                QuestionStatus.valueOf((String) row[0]),
                ((Number) row[1]).longValue()))
            .toList();
    }
}
