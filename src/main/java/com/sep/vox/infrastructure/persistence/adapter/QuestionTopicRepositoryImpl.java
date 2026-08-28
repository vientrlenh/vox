package com.sep.vox.infrastructure.persistence.adapter;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.sep.vox.application.common.StringNormalization;
import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.question.QuestionTopic;
import com.sep.vox.domain.model.question.QuestionTopicStatus;
import com.sep.vox.domain.repository.QuestionTopicRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionTopicMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionTopicRepository;

@Repository
public class QuestionTopicRepositoryImpl implements QuestionTopicRepository {

    private final SpringDataQuestionTopicRepository springDataQuestionTopicRepository;

    public QuestionTopicRepositoryImpl(SpringDataQuestionTopicRepository springDataQuestionTopicRepository) {
        this.springDataQuestionTopicRepository = springDataQuestionTopicRepository;
    }

    @Override
    public QuestionTopic save(QuestionTopic questionTopic) {
        var entity = QuestionTopicMapper.toJpa(questionTopic);
        var saved = springDataQuestionTopicRepository.save(entity);
        return QuestionTopicMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionTopic> findById(UUID id) {
        return springDataQuestionTopicRepository.findById(id)
            .map(QuestionTopicMapper::toDomain);
    }

    @Override
    public List<QuestionTopic> findByIdIn(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return springDataQuestionTopicRepository.findAllById(ids).stream()
            .map(QuestionTopicMapper::toDomain)
            .toList();
    }

    @Override
    public List<QuestionTopic> findByQuestionBankId(UUID questionBankId) {
        return springDataQuestionTopicRepository.findByQuestionBankId(questionBankId).stream()
            .map(QuestionTopicMapper::toDomain)
            .toList();
    }

    @Override
    public PageResult<QuestionTopic> findByQuestionBankId(UUID questionBankId, int pageNumber, int size) {
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataQuestionTopicRepository.findByQuestionBankId(questionBankId, pageable);
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionTopicMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }

    @Override
    public boolean existsById(UUID id) {
        return springDataQuestionTopicRepository.existsById(id);
    }

    @Override
    public boolean isTopicBelongToSchool(UUID id, UUID schoolId) {
        return springDataQuestionTopicRepository.isTopicBelongToSchool(id, schoolId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionTopicRepository.deleteById(id);
    }

    @Override
    public PageResult<QuestionTopic> findAccessible(UUID currentSchoolId, boolean systemAdmin, boolean schoolAdmin,
            UUID questionBankId, QuestionTopicStatus status, String keyword, int pageNumber, int size) {
        // pageNumber là 1-based như mọi truy vấn phân trang khác của dự án; Spring Data đếm từ 0.
        //
        // Đợt refactor subscription đã lật 35 trường phân trang trên schema GraphQL sang 1-based
        // nhưng KHÔNG sửa các adapter tương ứng, nên một thời gian dài schema hứa 1-based còn ở đây
        // vẫn tính từ 0: trang đầu client xin về lại ra trang thứ hai. Không có lỗi nào nổi lên vì
        // kiểu dữ liệu không đổi -- chỉ có dữ liệu sai. Chỗ này là một trong 16 adapter đã kéo về
        // cho khớp; quy ước chung là adapter luôn nhận 1-based và tự trừ.
        var pageable = PageRequest.of(pageNumber - 1, size);
        var page = springDataQuestionTopicRepository.findAccessible(
            currentSchoolId,
            systemAdmin,
            schoolAdmin,
            questionBankId,
            status == null ? null : status.name(),
            StringNormalization.toLikePattern(keyword),
            pageable
        );
        return new PageResult<>(
            page.getContent().stream()
                .map(QuestionTopicMapper::toDomain)
                .toList(),
            pageNumber,
            size,
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
