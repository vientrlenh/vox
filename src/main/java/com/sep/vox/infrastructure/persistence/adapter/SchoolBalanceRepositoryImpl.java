package com.sep.vox.infrastructure.persistence.adapter;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.sep.vox.domain.model.school.SchoolBalance;
import com.sep.vox.domain.repository.SchoolBalanceRepository;
import com.sep.vox.infrastructure.persistence.mapper.SchoolBalanceMapper;
import com.sep.vox.infrastructure.persistence.repository.SpringDataSchoolBalanceRepository;

@Repository
public class SchoolBalanceRepositoryImpl implements SchoolBalanceRepository {

    private final SpringDataSchoolBalanceRepository springDataSchoolBalanceRepository;

    public SchoolBalanceRepositoryImpl(SpringDataSchoolBalanceRepository springDataSchoolBalanceRepository) {
        this.springDataSchoolBalanceRepository = springDataSchoolBalanceRepository;
    }

    @Override
    public Optional<SchoolBalance> findById(UUID id) {
        return springDataSchoolBalanceRepository.findById(id).map(SchoolBalanceMapper::toDomain);
    }

    @Override
    public Optional<SchoolBalance> findBySchoolId(UUID schoolId) {
        return springDataSchoolBalanceRepository.findBySchoolId(schoolId).map(SchoolBalanceMapper::toDomain);
    }

    /**
     * INSERT ... ON CONFLICT DO NOTHING rồi mới khóa: hai câu, nhưng câu đầu chỉ ghi đúng một lần cho
     * cả đời của trường và các lần sau chỉ tốn một lần kiểm ràng buộc duy nhất.
     */
    @Override
    public SchoolBalance findBySchoolIdForUpdateOrCreate(UUID schoolId, Instant now) {
        springDataSchoolBalanceRepository.insertIfAbsent(schoolId, now);
        return springDataSchoolBalanceRepository.findWithLockBySchoolId(schoolId)
            .map(SchoolBalanceMapper::toDomain)
            .orElseThrow(() -> new IllegalStateException(
                "Không đọc được ví trường " + schoolId + " ngay sau khi đã bảo đảm dòng tồn tại"));
    }

    @Override
    public SchoolBalance save(SchoolBalance balance) {
        var entity = SchoolBalanceMapper.toJpa(balance);
        var saved = springDataSchoolBalanceRepository.save(entity);
        return SchoolBalanceMapper.toDomain(saved);
    }
}
