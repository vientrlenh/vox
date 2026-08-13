package com.sep.vox.infrastructure.initializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.sep.vox.domain.model.framework.Framework;
import com.sep.vox.domain.model.framework.FrameworkCriterion;
import com.sep.vox.domain.model.framework.FrameworkCriterionBand;
import com.sep.vox.domain.model.framework.FrameworkResultBand;
import com.sep.vox.domain.model.framework.FrameworkVersion;
import com.sep.vox.domain.model.framework.FrameworkVersionStatus;
import com.sep.vox.domain.repository.FrameworkCriterionBandRepository;
import com.sep.vox.domain.repository.FrameworkCriterionRepository;
import com.sep.vox.domain.repository.FrameworkRepository;
import com.sep.vox.domain.repository.FrameworkResultBandRepository;
import com.sep.vox.domain.repository.FrameworkVersionRepository;
import com.sep.vox.domain.valueobject.FrameworkCode;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignal;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignalImportance;
import com.sep.vox.domain.valueobject.framework.FrameworkCriterionSignals;

/**
 * Dựng sẵn khung đánh giá CEFR để hệ thống chấm được ngay từ lần khởi động đầu.
 *
 * <p>Không có bản này thì {@code FrameworkVersionRepository.findActiveVersionId} trả rỗng và
 * mọi phiên luyện tập đều không vào được -- nên đây là dữ liệu bắt buộc, không phải demo.
 *
 * <p>Bản dựng ra đã ở trạng thái PUBLISHED và thoả đúng những điều kiện mà
 * {@code UpdateFrameworkVersionStatusUseCase} bắt buộc khi xuất bản: đủ và chỉ 5 tiêu chí
 * theo {@code FrameworkCriterionCode.ALLOWED_CODES}, mỗi tiêu chí có đủ 6 thang, mỗi thang
 * có ít nhất một dấu hiệu tích cực và một dấu hiệu tiêu cực. Ghi thẳng qua repository là
 * đường tắt vòng qua use case đó, nên các bất biến phải được giữ bằng tay ở đây.
 */
@Component
@Order(4)
public class FrameworkInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkInitializer.class);

    /**
     * Sử dụng khung đánh giá khởi tạo mặc định là CEFR, với 5 tiêu chí: Pronunciation, Fluency, Grammar, Vocabulary, Coherence
     */
    private static final String FRAMEWORK_CODE = "CEFR";

    // Mặc định khởi tạo là version 1
    private static final String FRAMEWORK_VERSION_CODE = "CEFR_V1";

    // Các tiêu chí của chuẩn, bắt buộc phải đúng 5 code ở từng tiêu chí
    // Hiện tại, do những hạn chế về cấu hình của Service AI chấm điểm, nếu các code này định nghĩa sai, model sẽ không thể đọc và đưa ra kết quả đánh giá đúng
    //
    // Năm hằng số này PHẢI trùng khít FrameworkCriterionCode.ALLOWED_CODES: lúc xuất bản,
    // UpdateFrameworkVersionStatusUseCase so sánh hai tập bằng equals(), nên lệch một ký tự
    // là không version nào xuất bản được nữa. PracticeQuestionSelectionService và
    // SubAttributePolicy cũng tra theo đúng các chuỗi đó.
    private static final String FRAMEWORK_VERSION_CRITERION_CODE_PRONUNCIATION = "PRONUNCIATION";
    private static final String FRAMEWORK_VERSION_CRITERION_CODE_FLUENCY = "FLUENCY";
    private static final String FRAMEWORK_VERSION_CRITERION_CODE_GRAMMAR = "GRAMMAR";
    private static final String FRAMEWORK_VERSION_CRITERION_CODE_VOCABULARY = "VOCABULARY";
    private static final String FRAMEWORK_VERSION_CRITERION_CODE_COHERENCE = "COHERENCE";

    // Các band đánh giá trong khung CEFR, từ A1 đến C2
    private static final String FRAMEWORK_VERSION_BAND_CODE_A1 = "A1";
    private static final String FRAMEWORK_VERSION_BAND_CODE_A2 = "A2";
    private static final String FRAMEWORK_VERSION_BAND_CODE_B1 = "B1";
    private static final String FRAMEWORK_VERSION_BAND_CODE_B2 = "B2";
    private static final String FRAMEWORK_VERSION_BAND_CODE_C1 = "C1";
    private static final String FRAMEWORK_VERSION_BAND_CODE_C2 = "C2";

    private static final List<CriterionSeed> CRITERIA = List.of(
        new CriterionSeed(FRAMEWORK_VERSION_CRITERION_CODE_PRONUNCIATION, "Phát âm",
            "Độ chính xác của âm, trọng âm và ngữ điệu; mức công sức người nghe phải bỏ ra để hiểu.", 1),
        new CriterionSeed(FRAMEWORK_VERSION_CRITERION_CODE_FLUENCY, "Độ trôi chảy",
            "Tốc độ, nhịp nói và cách xử lý khi ngập ngừng hoặc phải sửa lời.", 2),
        new CriterionSeed(FRAMEWORK_VERSION_CRITERION_CODE_GRAMMAR, "Ngữ pháp",
            "Độ chính xác và độ đa dạng của cấu trúc câu được dùng.", 3),
        new CriterionSeed(FRAMEWORK_VERSION_CRITERION_CODE_VOCABULARY, "Từ vựng",
            "Độ rộng của vốn từ và độ chuẩn xác khi chọn từ theo ngữ cảnh.", 4),
        new CriterionSeed(FRAMEWORK_VERSION_CRITERION_CODE_COHERENCE, "Tính mạch lạc",
            "Cách tổ chức ý và dùng liên từ để người nghe theo được mạch lập luận.", 5)
    );

    private static final List<BandSeed> RESULT_BANDS = List.of(
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_A1, "A1 - Mới bắt đầu",
            "Giao tiếp được ở mức từ và cụm từ rời rạc trong tình huống rất quen thuộc.", 1),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_A2, "A2 - Sơ cấp",
            "Trao đổi được thông tin đơn giản về chủ đề quen thuộc bằng câu ngắn.", 2),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_B1, "B1 - Trung cấp",
            "Trình bày được trải nghiệm, kế hoạch và lý do ở mức đủ rõ để người nghe theo kịp.", 3),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_B2, "B2 - Trung cao cấp",
            "Thảo luận trôi chảy nhiều chủ đề, bảo vệ được quan điểm bằng lập luận có cấu trúc.", 4),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_C1, "C1 - Cao cấp",
            "Diễn đạt linh hoạt và chính xác, xử lý được cả chủ đề trừu tượng.", 5),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_C2, "C2 - Thành thạo",
            "Diễn đạt tự nhiên, chính xác và có sắc thái gần với người bản ngữ.", 6)
    );

    private final FrameworkRepository frameworkRepository;
    private final FrameworkVersionRepository frameworkVersionRepository;
    private final FrameworkCriterionRepository frameworkCriterionRepository;
    private final FrameworkResultBandRepository frameworkResultBandRepository;
    private final FrameworkCriterionBandRepository frameworkCriterionBandRepository;

    public FrameworkInitializer(
            FrameworkRepository frameworkRepository,
            FrameworkVersionRepository frameworkVersionRepository,
            FrameworkCriterionRepository frameworkCriterionRepository,
            FrameworkResultBandRepository frameworkResultBandRepository,
            FrameworkCriterionBandRepository frameworkCriterionBandRepository) {
        this.frameworkRepository = frameworkRepository;
        this.frameworkVersionRepository = frameworkVersionRepository;
        this.frameworkCriterionRepository = frameworkCriterionRepository;
        this.frameworkResultBandRepository = frameworkResultBandRepository;
        this.frameworkCriterionBandRepository = frameworkCriterionBandRepository;
    }

    /**
     * {@code @Transactional} ở đây không phải để tối ưu: dữ liệu này là một đồ thị có khoá
     * ngoại (framework -> version -> criterion/result band -> criterion band). Nếu tiến
     * trình chết giữa chừng mà không cuộn lại, lần khởi động sau sẽ thấy framework đã tồn
     * tại rồi bỏ qua, để lại một bản dựng dở không bao giờ được sửa.
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (frameworkRepository.findByCode(FRAMEWORK_CODE).isPresent()) {
            LOGGER.info("Framework {} đã tồn tại. Bỏ qua khởi tạo khung đánh giá", FRAMEWORK_CODE);
            return;
        }

        var now = Instant.now();

        var framework = frameworkRepository.save(new Framework(
            new FrameworkCode(FRAMEWORK_CODE),
            "Khung tham chiếu ngôn ngữ chung châu Âu (CEFR)",
            "Khung đánh giá năng lực nói mặc định của hệ thống, gồm 5 tiêu chí và 6 thang từ A1 đến C2.",
            true,
            now, now, null, null
        ));

        // effectiveFrom = now và effectiveTo = null là điều kiện để findActiveVersionId nhìn
        // thấy bản này; thiếu effectiveFrom thì luyện tập vẫn không chạy dù đã PUBLISHED.
        var version = frameworkVersionRepository.save(new FrameworkVersion(
            framework.getId(),
            FRAMEWORK_VERSION_CODE,
            "CEFR phiên bản 1",
            "Phiên bản khởi tạo, dùng cho cả đường thi lẫn đường luyện tập.",
            1,
            now,
            null,
            FrameworkVersionStatus.PUBLISHED,
            now, now, null, null
        ));

        var criteria = frameworkCriterionRepository.saveAll(CRITERIA.stream()
            .map(seed -> new FrameworkCriterion(
                version.getId(), seed.code(), seed.name(), seed.description(), seed.order(),
                now, now, null, null))
            .toList());

        var resultBands = frameworkResultBandRepository.saveAll(RESULT_BANDS.stream()
            .map(seed -> new FrameworkResultBand(
                version.getId(), seed.code(), seed.label(), seed.description(), seed.order(),
                now, now, null, null))
            .toList());

        frameworkCriterionBandRepository.saveAll(buildCriterionBands(criteria, resultBands, now));

        LOGGER.info("Đã khởi tạo framework {} ({}) với {} tiêu chí và {} thang kết quả",
            FRAMEWORK_CODE, FRAMEWORK_VERSION_CODE, criteria.size(), resultBands.size());
    }

    /**
     * Tích Descartes 5 tiêu chí x 6 thang = 30 dòng. Mỗi dòng bắt buộc có descriptor cùng ít
     * nhất một dấu hiệu tích cực và một dấu hiệu tiêu cực -- thiếu bất kỳ phần nào thì bản
     * này không còn hợp lệ để ở trạng thái PUBLISHED.
     */
    private List<FrameworkCriterionBand> buildCriterionBands(
            List<FrameworkCriterion> criteria, List<FrameworkResultBand> resultBands, Instant now) {
        var contentByCriterion = bandContent();
        var bands = new ArrayList<FrameworkCriterionBand>();

        for (var criterion : criteria) {
            var contentByBand = contentByCriterion.get(criterion.getCode());
            for (var resultBand : resultBands) {
                var content = contentByBand.get(resultBand.getCode());
                bands.add(new FrameworkCriterionBand(
                    criterion.getId(),
                    resultBand.getId(),
                    content.descriptor(),
                    signal(criterion.getCode() + "_" + resultBand.getCode() + "_POS", content.positive()),
                    signal(criterion.getCode() + "_" + resultBand.getCode() + "_NEG", content.negative()),
                    now, now, null, null
                ));
            }
        }
        return bands;
    }

    private FrameworkCriterionSignals signal(String code, String description) {
        return new FrameworkCriterionSignals(List.of(new FrameworkCriterionSignal(
            code, description, FrameworkCriterionSignalImportance.HIGH, null)));
    }

    private Map<String, Map<String, BandContent>> bandContent() {
        var content = new LinkedHashMap<String, Map<String, BandContent>>();

        content.put(FRAMEWORK_VERSION_CRITERION_CODE_PRONUNCIATION, Map.of(
            FRAMEWORK_VERSION_BAND_CODE_A1, new BandContent(
                "Phát âm rời rạc, sai nhiều âm; người nghe phải suy đoán mới hiểu.",
                "Đọc đúng được một số từ quen thuộc khi đứng riêng lẻ.",
                "Sai âm cuối và trọng âm ở hầu hết các từ."),
            FRAMEWORK_VERSION_BAND_CODE_A2, new BandContent(
                "Phát âm hiểu được ở câu ngắn quen thuộc, ảnh hưởng tiếng mẹ đẻ còn rõ.",
                "Phát âm đúng phần lớn từ thông dụng trong câu ngắn.",
                "Nuốt âm cuối tới mức làm lệch nghĩa của từ."),
            FRAMEWORK_VERSION_BAND_CODE_B1, new BandContent(
                "Phát âm rõ ở phần lớn phát ngôn; người nghe quen tai vẫn theo được trọn ý.",
                "Đặt trọng âm từ chính xác ở đa số trường hợp.",
                "Ngữ điệu phẳng, một vài chỗ khiến người nghe phải hỏi lại."),
            FRAMEWORK_VERSION_BAND_CODE_B2, new BandContent(
                "Phát âm rõ ràng; trọng âm và ngữ điệu đủ để làm nổi ý muốn nhấn.",
                "Dùng ngữ điệu để làm nổi bật thông tin quan trọng.",
                "Còn lỗi lẻ tẻ ở các cụm phụ âm khó."),
            FRAMEWORK_VERSION_BAND_CODE_C1, new BandContent(
                "Phát âm tự nhiên, điều chỉnh được ngữ điệu theo sắc thái muốn truyền đạt.",
                "Thay đổi nhịp và trọng âm câu linh hoạt theo ý đồ.",
                "Thỉnh thoảng lộ dấu vết tiếng mẹ đẻ ở từ hiếm gặp."),
            FRAMEWORK_VERSION_BAND_CODE_C2, new BandContent(
                "Phát âm gần như người bản ngữ, không tạo tải nghe cho người đối thoại.",
                "Kiểm soát ngữ điệu tinh tế để truyền được cả hàm ý.",
                "Không còn lỗi nào ảnh hưởng tới việc hiểu.")
        ));

        content.put(FRAMEWORK_VERSION_CRITERION_CODE_FLUENCY, Map.of(
            FRAMEWORK_VERSION_BAND_CODE_A1, new BandContent(
                "Nói ngắt quãng liên tục, chủ yếu là từ đơn lẻ và khoảng lặng dài.",
                "Bật ra được từ cần dùng sau khi nghĩ.",
                "Khoảng lặng dài tới mức làm đứt mạch giao tiếp."),
            FRAMEWORK_VERSION_BAND_CODE_A2, new BandContent(
                "Nói được câu ngắn nhưng ngập ngừng rõ khi phải tìm từ.",
                "Duy trì được lượt nói ngắn về chủ đề quen thuộc.",
                "Lặp lại và tự sửa nhiều, làm chậm hẳn nhịp nói."),
            FRAMEWORK_VERSION_BAND_CODE_B1, new BandContent(
                "Nói liên tục được đoạn dài vừa phải, ngập ngừng chủ yếu ở chỗ chuyển ý.",
                "Giữ được mạch nói mà không cần người nghe nhắc.",
                "Nhịp chậm lại rõ rệt khi gặp chủ đề ngoài vùng quen thuộc."),
            FRAMEWORK_VERSION_BAND_CODE_B2, new BandContent(
                "Nói trôi chảy ở nhịp gần tự nhiên, ngập ngừng không cản người nghe.",
                "Tự sửa gọn mà không làm gãy mạch trình bày.",
                "Còn vài chỗ dừng để tìm cấu trúc phù hợp."),
            FRAMEWORK_VERSION_BAND_CODE_C1, new BandContent(
                "Nói trôi chảy và đều nhịp, kể cả khi triển khai lập luận dài.",
                "Chủ động dùng quãng nghỉ như một công cụ nhấn ý.",
                "Rất ít khi phải dừng lại tìm cách diễn đạt."),
            FRAMEWORK_VERSION_BAND_CODE_C2, new BandContent(
                "Nói tự nhiên và liền mạch như trong hội thoại đời thường.",
                "Điều tiết nhịp nói theo nội dung một cách có chủ đích.",
                "Không có ngập ngừng nào do thiếu phương tiện diễn đạt.")
        ));

        content.put(FRAMEWORK_VERSION_CRITERION_CODE_GRAMMAR, Map.of(
            FRAMEWORK_VERSION_BAND_CODE_A1, new BandContent(
                "Chủ yếu dùng cụm từ học thuộc, cấu trúc câu chưa hình thành rõ.",
                "Ghép được chủ ngữ và động từ trong mẫu câu quen.",
                "Sai cơ bản ở thì và số ít/số nhiều gây khó hiểu."),
            FRAMEWORK_VERSION_BAND_CODE_A2, new BandContent(
                "Dùng đúng một số cấu trúc đơn giản trong ngữ cảnh quen thuộc.",
                "Dùng đúng thì hiện tại và quá khứ đơn ở câu ngắn.",
                "Câu ghép thường sai trật tự hoặc thiếu thành phần."),
            FRAMEWORK_VERSION_BAND_CODE_B1, new BandContent(
                "Dùng được nhiều cấu trúc thông dụng, lỗi còn nhưng ít cản trở việc hiểu.",
                "Kết hợp được mệnh đề phụ vào câu một cách hợp lý.",
                "Sai lặp ở một vài điểm ngữ pháp nhất định."),
            FRAMEWORK_VERSION_BAND_CODE_B2, new BandContent(
                "Kiểm soát tốt ngữ pháp; lỗi thưa và hiếm khi gây hiểu sai.",
                "Dùng đa dạng cấu trúc phù hợp với ý muốn diễn đạt.",
                "Còn lỗi ở cấu trúc phức tạp khi nói nhanh."),
            FRAMEWORK_VERSION_BAND_CODE_C1, new BandContent(
                "Ngữ pháp chính xác và linh hoạt, kể cả ở câu dài nhiều tầng.",
                "Chọn cấu trúc phục vụ sắc thái chứ không chỉ để đúng.",
                "Lỗi lẻ tẻ, tự nhận ra và sửa ngay."),
            FRAMEWORK_VERSION_BAND_CODE_C2, new BandContent(
                "Ngữ pháp chuẩn xác một cách nhất quán ở mọi độ phức tạp.",
                "Vận dụng cấu trúc tinh tế để tạo hiệu quả diễn đạt.",
                "Gần như không có lỗi ngay cả khi nói tự phát.")
        ));

        content.put(FRAMEWORK_VERSION_CRITERION_CODE_VOCABULARY, Map.of(
            FRAMEWORK_VERSION_BAND_CODE_A1, new BandContent(
                "Vốn từ rất hạn chế, chỉ đủ cho nhu cầu cơ bản nhất.",
                "Dùng đúng một số từ về bản thân và môi trường quanh mình.",
                "Thiếu từ tới mức phải bỏ dở ý đang nói."),
            FRAMEWORK_VERSION_BAND_CODE_A2, new BandContent(
                "Đủ từ cho chủ đề quen thuộc, còn lặp và diễn giải vòng vo.",
                "Dùng đúng từ thông dụng theo chủ đề đã học.",
                "Lặp lại một nhóm từ nhỏ do không có lựa chọn thay thế."),
            FRAMEWORK_VERSION_BAND_CODE_B1, new BandContent(
                "Đủ từ để nói về chủ đề quen thuộc mà không phải né tránh ý.",
                "Diễn giải được ý khi chưa nhớ ra từ chính xác.",
                "Chọn từ chưa chuẩn xác ở chủ đề ngoài vùng quen thuộc."),
            FRAMEWORK_VERSION_BAND_CODE_B2, new BandContent(
                "Vốn từ rộng, chọn từ phù hợp với phần lớn ngữ cảnh.",
                "Dùng được cụm từ cố định và từ nối tự nhiên.",
                "Đôi chỗ dùng từ chưa khớp sắc thái của ngữ cảnh."),
            FRAMEWORK_VERSION_BAND_CODE_C1, new BandContent(
                "Vốn từ phong phú, chọn từ chính xác kể cả ở chủ đề trừu tượng.",
                "Dùng thành ngữ và cách nói ẩn dụ đúng chỗ.",
                "Hiếm khi phải diễn giải vòng vì thiếu từ."),
            FRAMEWORK_VERSION_BAND_CODE_C2, new BandContent(
                "Vốn từ rất rộng và chính xác, phân biệt được sắc thái tinh tế.",
                "Chọn từ có chủ đích để tạo hiệu quả riêng cho người nghe.",
                "Không có dấu hiệu bị giới hạn bởi vốn từ.")
        ));

        content.put(FRAMEWORK_VERSION_CRITERION_CODE_COHERENCE, Map.of(
            FRAMEWORK_VERSION_BAND_CODE_A1, new BandContent(
                "Các ý rời rạc, chưa có liên kết giữa các câu.",
                "Sắp xếp được hai ý theo trình tự dễ hiểu.",
                "Không dùng từ nối, người nghe phải tự ghép ý."),
            FRAMEWORK_VERSION_BAND_CODE_A2, new BandContent(
                "Nối được các câu bằng vài liên từ cơ bản.",
                "Dùng đúng các liên từ như và, nhưng, vì.",
                "Mạch ý đứt quãng khi đoạn nói dài hơn vài câu."),
            FRAMEWORK_VERSION_BAND_CODE_B1, new BandContent(
                "Trình bày theo trình tự rõ, người nghe theo được mạch chính.",
                "Dẫn dắt từ ý này sang ý khác một cách hợp lý.",
                "Chuyển ý còn đột ngột ở một vài chỗ."),
            FRAMEWORK_VERSION_BAND_CODE_B2, new BandContent(
                "Bố cục rõ ràng, các ý được liên kết bằng phương tiện đa dạng.",
                "Dùng từ nối phong phú để đánh dấu quan hệ giữa các ý.",
                "Một vài đoạn triển khai chưa cân đối về độ dài."),
            FRAMEWORK_VERSION_BAND_CODE_C1, new BandContent(
                "Lập luận chặt chẽ, mạch ý được duy trì xuyên suốt đoạn dài.",
                "Dùng cấu trúc diễn ngôn để dẫn người nghe theo lập luận.",
                "Rất hiếm chỗ khiến người nghe mất mạch."),
            FRAMEWORK_VERSION_BAND_CODE_C2, new BandContent(
                "Tổ chức ý mạch lạc và tự nhiên như một bài nói đã chuẩn bị.",
                "Điều hướng mạch ý linh hoạt mà vẫn giữ được trọng tâm.",
                "Không có chỗ nào làm gián đoạn mạch hiểu của người nghe.")
        ));

        return content;
    }

    private record CriterionSeed(String code, String name, String description, int order) {}

    private record BandSeed(String code, String label, String description, int order) {}

    private record BandContent(String descriptor, String positive, String negative) {}
}
