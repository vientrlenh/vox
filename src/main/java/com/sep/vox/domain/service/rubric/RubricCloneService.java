package com.sep.vox.domain.service.rubric;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.rubric.Rubric;
import com.sep.vox.domain.model.rubric.RubricCriterion;
import com.sep.vox.domain.model.rubric.RubricOwnerType;
import com.sep.vox.domain.model.rubric.RubricResultBand;
import com.sep.vox.domain.model.rubric.RubricStatus;
import com.sep.vox.domain.model.rubric.RubricTotalScoreMethod;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.domain.repository.RubricCriterionRepository;
import com.sep.vox.domain.repository.RubricRepository;
import com.sep.vox.domain.repository.RubricResultBandRepository;
import com.sep.vox.domain.repository.RubricVersionRepository;

/**
 * Sao một phiên bản rubric của hệ thống thành rubric riêng của một trường.
 *
 * <p>Sao sâu bốn tầng: {@link Rubric} -> {@link RubricVersion} -> {@link RubricCriterion} +
 * {@link RubricResultBand}. Bản sao là tài sản độc lập của trường ngay từ khoảnh khắc tạo ra: sửa nó
 * không đụng tới bản mẫu, và bản mẫu đổi về sau cũng không tự chảy sang.
 */
@Service
public class RubricCloneService {

    private final RubricRepository rubricRepository;
    private final RubricVersionRepository rubricVersionRepository;
    private final RubricCriterionRepository rubricCriterionRepository;
    private final RubricResultBandRepository rubricResultBandRepository;

    public RubricCloneService(
            RubricRepository rubricRepository,
            RubricVersionRepository rubricVersionRepository,
            RubricCriterionRepository rubricCriterionRepository,
            RubricResultBandRepository rubricResultBandRepository) {
        this.rubricRepository = rubricRepository;
        this.rubricVersionRepository = rubricVersionRepository;
        this.rubricCriterionRepository = rubricCriterionRepository;
        this.rubricResultBandRepository = rubricResultBandRepository;
    }

    /**
     * @param targetMethod cách tính điểm trường chọn cho bản sao, có thể khác bản mẫu -- xem
     *                     {@link #resolveWeight}.
     * @return phiên bản vừa tạo, luôn ở trạng thái DRAFT.
     */
    @Transactional
    public RubricVersion cloneToSchoolAsDraft(
            Rubric sourceRubric,
            RubricVersion sourceVersion,
            UUID schoolId,
            String code,
            String name,
            String description,
            RubricTotalScoreMethod targetMethod,
            UUID currentUserId) {

        Instant now = Instant.now();

        // languageId and frameworkId follow the template and must NOT be chosen again: the criteria
        // below point to the frameworkCriterionId of the source framework, so changing the framework
        // would leave all of those references dangling.
        Rubric clonedRubric = rubricRepository.save(new Rubric(
            sourceRubric.getLanguageId(),
            sourceRubric.getFrameworkId(),
            code,
            name,
            description,
            RubricOwnerType.SCHOOL,
            schoolId
        ));

        RubricVersion clonedVersion = new RubricVersion(
            clonedRubric.getId(),
            // A new rubric always restarts versioning at 1. The version number is the TIME axis within
            // one rubric, not something inherited from the template -- a clone of "template v3" is
            // still the school's first version.
            1,
            code,
            name,
            description,
            // DRAFT is mandatory, not a convention: CreateSchoolAssessmentPolicyUseCase rejects
            // attaching a policy to an already PUBLISHED version, so a clone created directly as
            // PUBLISHED would be one the school can never attach any policy to -- i.e. useless.
            RubricStatus.DRAFT,
            now,
            null,
            sourceVersion.getScoringScaleMin(),
            sourceVersion.getScoringScaleMax(),
            targetMethod,
            now,
            now,
            currentUserId,
            currentUserId
        );
        clonedVersion.setSourceRubricVersionId(sourceVersion.getId());
        RubricVersion savedVersion = rubricVersionRepository.save(clonedVersion);

        cloneCriteria(sourceVersion.getId(), savedVersion.getId(), targetMethod, currentUserId, now);
        cloneResultBands(sourceVersion.getId(), savedVersion.getId(), currentUserId, now);

        return savedVersion;
    }

    private void cloneCriteria(
            UUID sourceVersionId, UUID targetVersionId, RubricTotalScoreMethod targetMethod,
            UUID currentUserId, Instant now) {
        List<RubricCriterion> sourceCriteria = rubricCriterionRepository.findByRubricVersionId(sourceVersionId);
        if (sourceCriteria.isEmpty()) {
            return;
        }
        rubricCriterionRepository.saveAll(sourceCriteria.stream()
            .map(source -> new RubricCriterion(
                targetVersionId,
                source.getFrameworkCriterionId(),
                source.getCode(),
                source.getName(),
                source.getDescription(),
                source.getExamples(),
                resolveWeight(source.getWeight(), targetMethod),
                source.getMinScore(),
                source.getMaxScore(),
                source.getOrder(),
                source.isRequired(),
                now,
                now,
                currentUserId,
                currentUserId
            ))
            .toList());
    }

    /**
     * Trọng số cho bản sao, theo cách tính mà trường chọn.
     *
     * <p>Hai cách tính có hai quy ước trọng số khác nhau, và chỉ một chiều chuyển đổi được:
     *
     * <ul>
     *   <li><b>Phân bổ (SUM)</b>: tổng trọng số bằng 100%, mỗi con số mang ý đồ về tầm quan trọng
     *       tương đối giữa các tiêu chí.
     *   <li><b>Trung bình (WEIGHTED_AVERAGE)</b>: mọi tiêu chí cân bằng ở 100%.
     * </ul>
     *
     * <p>Nên Trung bình suy được từ Phân bổ bằng cách san phẳng mọi trọng số về 1 -- đó là ý nghĩa
     * của việc chọn Trung bình, chứ không phải mất mát ngoài ý muốn. Chiều ngược lại thì không: từ
     * trạng thái mọi tiêu chí bằng nhau, không có cách nào tái tạo tỉ lệ mà bản mẫu định ra. Vì vậy
     * bản mẫu nên được soạn ở dạng Phân bổ -- nó mang nhiều thông tin hơn.
     *
     * <p>Kết quả luôn thoả {@link RubricScoringConsistencyValidator}, nên bản sao ban hành được ngay
     * sau khi trường gắn xong chính sách.
     */
    private BigDecimal resolveWeight(BigDecimal sourceWeight, RubricTotalScoreMethod targetMethod) {
        if (targetMethod == RubricTotalScoreMethod.WEIGHTED_AVERAGE) {
            return BigDecimal.ONE;
        }
        return sourceWeight;
    }

    private void cloneResultBands(UUID sourceVersionId, UUID targetVersionId, UUID currentUserId, Instant now) {
        List<RubricResultBand> sourceBands = rubricResultBandRepository.findByRubricVersionId(sourceVersionId);
        if (sourceBands.isEmpty()) {
            return;
        }
        rubricResultBandRepository.saveAll(sourceBands.stream()
            .map(source -> new RubricResultBand(
                targetVersionId,
                source.getCode(),
                source.getName(),
                source.getDescription(),
                source.getScoreMin(),
                source.getScoreMax(),
                source.getOrder(),
                now,
                now,
                currentUserId,
                currentUserId
            ))
            .toList());
    }
}
