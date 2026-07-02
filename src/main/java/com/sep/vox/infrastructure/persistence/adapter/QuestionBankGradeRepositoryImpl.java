package com.sep.vox.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.question.QuestionBankGrade;
import com.sep.vox.domain.repository.QuestionBankGradeRepository;
import com.sep.vox.infrastructure.persistence.mapper.QuestionBankGradeMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataQuestionBankGradeRepository;

@Repository
public class QuestionBankGradeRepositoryImpl implements QuestionBankGradeRepository {

    private final SpringDataQuestionBankGradeRepository springDataQuestionBankGradeRepository;

    public QuestionBankGradeRepositoryImpl(SpringDataQuestionBankGradeRepository springDataQuestionBankGradeRepository) {
        this.springDataQuestionBankGradeRepository = springDataQuestionBankGradeRepository;
    }

    @Override
    public QuestionBankGrade save(QuestionBankGrade grade) {
        var entity = QuestionBankGradeMapper.toJpa(grade);
        var saved = springDataQuestionBankGradeRepository.save(entity);
        return QuestionBankGradeMapper.toDomain(saved);
    }

    @Override
    public Optional<QuestionBankGrade> findById(UUID id) {
        return springDataQuestionBankGradeRepository.findById(id)
            .map(QuestionBankGradeMapper::toDomain);
    }

    @Override
    public List<QuestionBankGrade> findByQuestionBankId(UUID questionBankId) {
        return springDataQuestionBankGradeRepository.findByQuestionBankId(questionBankId).stream()
            .map(QuestionBankGradeMapper::toDomain)
            .toList();
    }

    @Override
    public boolean existsByQuestionBankIdAndSchoolGradeId(UUID questionBankId, UUID schoolGradeId) {
        return springDataQuestionBankGradeRepository.existsByQuestionBankIdAndSchoolGradeId(questionBankId, schoolGradeId);
    }

    @Override
    public void deleteById(UUID id) {
        springDataQuestionBankGradeRepository.deleteById(id);
    }

    @Override
    public void deleteByQuestionBankId(UUID questionBankId) {
        springDataQuestionBankGradeRepository.deleteByQuestionBankId(questionBankId);
    }
}
