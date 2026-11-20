package com.sep.vox.domain.repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import com.sep.vox.domain.model.exam.ExamResultStatusHistory;

/** Nhật ký đổi trạng thái bài thi — chỉ ghi thêm và đọc, không sửa, không xoá. */
public interface ExamResultStatusHistoryRepository {

    ExamResultStatusHistory save(ExamResultStatusHistory history);

    List<ExamResultStatusHistory> saveAll(List<ExamResultStatusHistory> histories);

    /** Dòng thời gian của một bài, cũ trước — thứ tự đọc tự nhiên của người xem. */
    List<ExamResultStatusHistory> findByCandidateResultIdOrderByCreatedAtAsc(UUID candidateResultId);

    /** Batch cho màn danh sách; gọi một lần thay vì lặp từng bài. */
    List<ExamResultStatusHistory> findByCandidateResultIdIn(Collection<UUID> candidateResultIds);

    /** Chỉ dùng khi xoá hẳn phiên thi; không phải đường sửa nhật ký. */
    void deleteByCandidateResultIdIn(Collection<UUID> candidateResultIds);
}
