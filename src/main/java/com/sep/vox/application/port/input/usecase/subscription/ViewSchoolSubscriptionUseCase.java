package com.sep.vox.application.port.input.usecase.subscription;

import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.port.input.query.ViewSchoolSubscriptionQuery;
import com.sep.vox.application.port.input.usecase.IUseCase;
import com.sep.vox.application.port.output.UserContextPort;
import com.sep.vox.domain.dto.SchoolSubscriptionDto;
import com.sep.vox.domain.repository.SchoolSubscriptionRepository;

/**
 * Kỳ đăng ký ĐANG CHẠY của một trường, hoặc null nếu trường chưa mua/đã hết hạn.
 *
 * <p>"Đang chạy" = status ACTIVE và hôm nay nằm trong khoảng hiệu lực (findActiveBySchoolId), tức
 * đúng cái kỳ mà mọi cửa chặn hạn mức đang soi. Một trường có thể có nhiều dòng chưa kết thúc -- kỳ
 * đang chạy cộng kỳ đã trả tiền đang xếp hàng -- nên trả "dòng mới nhất" sẽ cho ra kỳ chưa tới lượt
 * và nói sai về hạn mức hiện hành. Muốn nhìn cả dây thì dùng schoolSubscriptionHistory.
 *
 * <p>Cùng luật phân quyền với {@link ViewSchoolSubscriptionHistoryUseCase}: System Admin xem được mọi
 * trường, còn lại chỉ xem được trường của chính mình.
 */
@Service
public class ViewSchoolSubscriptionUseCase implements IUseCase<ViewSchoolSubscriptionQuery, SchoolSubscriptionDto> {

    private final SchoolSubscriptionRepository schoolSubscriptionRepository;
    private final UserContextPort userContextPort;

    public ViewSchoolSubscriptionUseCase(
            SchoolSubscriptionRepository schoolSubscriptionRepository,
            UserContextPort userContextPort) {
        this.schoolSubscriptionRepository = schoolSubscriptionRepository;
        this.userContextPort = userContextPort;
    }

    @Override
    public SchoolSubscriptionDto execute(ViewSchoolSubscriptionQuery input) {
        if (!userContextPort.isSystemAdmin() && !input.schoolId().equals(userContextPort.getCurrentSchoolId())) {
            throw new ForbiddenException("Quyền truy cập bị từ chối");
        }
        return schoolSubscriptionRepository.findActiveBySchoolId(input.schoolId())
            .map(SchoolSubscriptionDto::toDto)
            .orElse(null);
    }
}
