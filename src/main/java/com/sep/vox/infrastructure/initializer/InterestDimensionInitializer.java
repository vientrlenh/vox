package com.sep.vox.infrastructure.initializer;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.sep.vox.domain.model.personalization.InterestDimension;
import com.sep.vox.domain.repository.InterestDimensionRepository;

/**
 * Dựng sẵn danh mục chiều sở thích -- bảng nền của toàn bộ tính năng cá nhân hoá.
 *
 * <p>VÌ SAO CẦN: trước đây KHÔNG có đường nào nạp bảng này. Không migration nào {@code INSERT},
 * không initializer nào, và {@code vox-client-web} cũng chưa từng dựng màn quản trị cho nó (API
 * GraphQL thì có sẵn -- xem {@code InterestDimensionController}). Nghĩa là mọi cơ sở dữ liệu mới
 * đều có bảng này RỖNG.
 *
 * <p>Hậu quả đo được 2026-08-26 trên môi trường thật: học sinh vào tab luyện nói, bấm làm quiz sở
 * thích, và không có gì xảy ra. Không thông báo lỗi, không màn hình trắng -- chỉ là không vào
 * được. Log Java sạch, quyền được cấp bình thường, agents không nhận lời gọi nào. Truy ra thì
 * {@code interest_quiz_item} có 27 câu nhưng {@code interest_dimension} có 0 dòng.
 *
 * <p>Hỏng IM LẶNG là chỗ tệ nhất: {@code InterestQuizScorer.normalize} trả {@code Map.of()} khi
 * danh mục rỗng thay vì báo lỗi, nên không tầng nào phát hiện được. Mất khá nhiều công lần từ giao
 * diện xuống tận bảng dữ liệu mới thấy.
 *
 * <p>Idempotent theo TỪNG MÃ chứ không theo "bảng có rỗng không": thêm một chiều mới vào danh sách
 * dưới đây thì lần khởi động sau nó tự được bổ sung, trong khi các chiều quản trị viên đã sửa nhãn
 * qua giao diện vẫn giữ nguyên. Xét theo bảng rỗng thì cả hai điều đó đều hỏng.
 */
@Component
@Order(11)
public class InterestDimensionInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(InterestDimensionInitializer.class);

    /**
     * Sáu chiều đem ra hỏi trong quiz, cộng ACADEMIC_EXAM.
     *
     * <p>Sáu mã đầu KHÔNG phải chọn tuỳ ý: chúng là đúng tập mã mà {@code dimensions_json} của các
     * câu quiz do AI sinh ra đang tham chiếu (đối chiếu trên dữ liệu thật). Đổi mã ở đây mà không
     * sinh lại câu quiz là quiz mất điểm ở chiều tương ứng, lại theo kiểu im lặng.
     *
     * <p>ACADEMIC_EXAM khác hẳn nhóm còn lại: {@code quizEligible = false}. Đây là chiều hệ thống
     * TỰ gán cho chủ đề lấy từ ngân hàng đề ({@code ViewPracticeTopicOffersUseCase}), dùng để xếp
     * hạng chủ đề nhưng không phải sở thích nên không hỏi học sinh. Thiếu nó thì chủ đề từ ngân
     * hàng đề không xếp hạng được.
     */
    private static final List<InterestDimension> SEED = List.of(
        dimension("TECH_GAMING", "Công nghệ & trò chơi",
            "Thiết bị, phần mềm, game, xu hướng số", true, 1),
        dimension("SPORTS_HEALTH", "Thể thao & sức khoẻ",
            "Vận động, thi đấu, dinh dưỡng, lối sống lành mạnh", true, 2),
        dimension("TRAVEL_PLACES", "Du lịch & vùng đất",
            "Điểm đến, văn hoá vùng miền, trải nghiệm đi lại", true, 3),
        dimension("ENTERTAINMENT_MEDIA", "Giải trí & truyền thông",
            "Phim ảnh, âm nhạc, mạng xã hội, người nổi tiếng", true, 4),
        dimension("PEOPLE_SOCIETY", "Con người & xã hội",
            "Quan hệ, cộng đồng, vấn đề xã hội, giáo dục", true, 5),
        dimension("FUTURE_SCIENCE", "Khoa học & tương lai",
            "Khám phá khoa học, môi trường, xu hướng tương lai", true, 6),
        dimension("ACADEMIC_EXAM", "Học thuật & luyện đề",
            "Chủ đề lấy từ ngân hàng đề -- hệ thống tự gán, không hỏi học sinh", false, 7)
    );

    private static InterestDimension dimension(
            String code, String label, String description, boolean quizEligible, int displayOrder) {
        var now = Instant.now();
        return new InterestDimension(code, label, description, true, quizEligible, displayOrder, now, now);
    }

    private final InterestDimensionRepository interestDimensionRepository;

    public InterestDimensionInitializer(InterestDimensionRepository interestDimensionRepository) {
        this.interestDimensionRepository = interestDimensionRepository;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        var added = 0;
        for (var seed : SEED) {
            if (interestDimensionRepository.findByCode(seed.getCode()).isPresent()) {
                continue;
            }
            interestDimensionRepository.save(seed);
            added++;
        }

        if (added == 0) {
            LOGGER.info("Interest dimensions already present. Skip initialize");
            return;
        }
        LOGGER.info("Interest dimensions initialized successfully: them {} chieu moi", added);
    }
}
