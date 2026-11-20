package com.sep.vox.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.sep.vox.infrastructure.persistence.entity.SchoolJpaEntity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpringDataSchoolRepository extends JpaRepository<SchoolJpaEntity, UUID> {
    Optional<SchoolJpaEntity> findByCode(String code);

    List<SchoolJpaEntity> findByDomain(String domain);

    boolean existsByCode(String code);

    boolean existsByContactEmail(String contactEmail);

    boolean existsByContactPhone(String contactPhone);

    boolean existsByContactEmailAndIdNot(String email, UUID id);
    boolean existsByContactPhoneAndIdNot(String phone, UUID id);

    boolean existsByCodeAndIdNot(String normalizedCode, UUID id);
    boolean existsByIdAndIsActiveTrue(UUID schoolId);

    //COALESCE sẽ đóng vai trò hoạt động như sau : nhập name nếu ma null -> s.name
    //            s.name = COALESCE(:name, s.name, 'Vit'),
    //nếu mà s.name null -> Vit
    @Modifying
    @Query("""
            UPDATE SchoolJpaEntity s SET 
            s.name = COALESCE(:name, s.name),
            s.description = COALESCE(:description, s.description),
            s.contactPhone = COALESCE(:phone, s.contactPhone),
            s.contactEmail = COALESCE(:email, s.contactEmail),
            s.domain = COALESCE(:domain, s.domain),
            s.address = COALESCE(:address, s.address),
            s.studentCount = COALESCE(:studentCount, s.studentCount),
            s.updatedAt = :updatedAt,
            s.updatedBy = :updatedBy
            WHERE s.id = :id
            """)
    int updateSchoolAtomic(
            @Param("id") UUID id,
            @Param("name") String name,
            @Param("description") String description,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("domain") String domain,
            @Param("address") String address,
            @Param("studentCount") Integer studentCount,
            @Param("updatedAt") Instant updatedAt,
            @Param("updatedBy") UUID updatedBy
    );
    List<SchoolJpaEntity> findByIdIn(Collection<UUID> ids);

    long countByIsActiveTrue();

    @Query("SELECT s FROM SchoolJpaEntity s WHERE " +
           "(:search IS NULL OR LOWER(s.name) LIKE :search ESCAPE '!' OR LOWER(s.code) LIKE :search ESCAPE '!')")
    Page<SchoolJpaEntity> findAllBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT s FROM SchoolJpaEntity s WHERE " +
           "(:search IS NULL OR LOWER(s.name) LIKE :search ESCAPE '!' OR LOWER(s.code) LIKE :search ESCAPE '!') " +
           "AND s.isActive = :isActive")
    Page<SchoolJpaEntity> findAllBySearchAndIsActive(@Param("search") String search, @Param("isActive") boolean isActive, Pageable pageable);

    /**
     * Trường đang có ca thi DIỄN RA: đã công bố, đã tới giờ, chưa hết giờ.
     *
     * <p>Soi LỊCH THI chứ không soi phiên thi đang mở, vì đây phải là ĐÚNG vị từ mà
     * {@code ForceSuspendSubscriptionUseCase} dùng để từ chối đình chỉ -- nó gọi
     * {@code ExamSchedule.isOngoingAt}, tức trạng thái của LỊCH. Đếm theo phiên (status =
     * 'IN_PROGRESS') sẽ bỏ sót đúng ca hay gặp nhất: cửa thi vừa mở mà chưa em nào bấm vào. Lúc đó
     * BE vẫn chặn nhưng danh sách này rỗng, nên nút hiện bật, admin bấm, rồi mới nhận lỗi -- đúng
     * thứ mà trạng thái chặn sinh ra để tránh.
     *
     * <p>Cùng biểu thức với {@code SpringDataExamScheduleRepository.findByExamIdAndInSchedule}, chỉ
     * khác là gom theo trường thay vì theo một bài thi.
     */
    @Query("""
        SELECT DISTINCT exam.schoolId
        FROM ExamScheduleJpaEntity schedule
        JOIN ExamJpaEntity exam
            ON schedule.examId = exam.id
        WHERE schedule.status = 'PUBLISHED'
            AND schedule.startDate <= :now
            AND schedule.endDate > :now
        """)
    List<UUID> findIdsWithOngoingExam(@Param("now") Instant now);
}
