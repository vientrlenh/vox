package com.sep.vox.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.sep.vox.domain.common.PageResult;
import com.sep.vox.domain.model.rubric.RubricVersion;
import com.sep.vox.infrastructure.persistence.entity.RubricVersionJpaEntity;
import com.sep.vox.infrastructure.persistence.repository.SpringDataRubricVersionRepository;

/**
 * The API pages are 1-based and Spring Data's are 0-based. Spring Data answers the first page with
 * {@code getNumber() == 0}, so every paged method must answer {@code page == 1} for it.
 */
@ExtendWith(MockitoExtension.class)
class RubricVersionRepositoryImplPagingTests {

    private static final int FIRST_PAGE = 1;
    private static final int SIZE = 10;

    @Mock
    private SpringDataRubricVersionRepository springDataRubricVersionRepository;

    @InjectMocks
    private RubricVersionRepositoryImpl rubricVersionRepository;

    private final ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

    @Test
    void should_answer_page_one_when_finding_all_by_rubric_and_status() {
        when(springDataRubricVersionRepository.findAllByRubricIdAndStatus(any(), any(), any(Pageable.class)))
            .thenReturn(firstSpringPage());

        PageResult<RubricVersion> result = rubricVersionRepository.findAllByRubricIdAndStatus(
            UUID.randomUUID(), null, FIRST_PAGE, SIZE);

        verify(springDataRubricVersionRepository).findAllByRubricIdAndStatus(any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(result.page()).isEqualTo(FIRST_PAGE);
    }

    @Test
    void should_answer_page_one_when_searching_rubric_versions() {
        when(springDataRubricVersionRepository.searchRubricVersions(any(), any(), any(), any(Pageable.class)))
            .thenReturn(firstSpringPage());

        PageResult<RubricVersion> result = rubricVersionRepository.searchRubricVersions(
            UUID.randomUUID(), null, null, FIRST_PAGE, SIZE);

        verify(springDataRubricVersionRepository).searchRubricVersions(any(), any(), any(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        assertThat(result.page()).isEqualTo(FIRST_PAGE);
    }

    private static Page<RubricVersionJpaEntity> firstSpringPage() {
        return new PageImpl<>(List.of(), PageRequest.of(0, SIZE), 0);
    }
}
