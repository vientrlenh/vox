package com.sep.vox.infrastructure.initializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Dựng sẵn Khung năng lực ngoại ngữ 6 bậc dùng cho Việt Nam (KNLNNVN) để hệ thống chấm được
 * ngay từ lần khởi động đầu.
 *
 * <p>Không có bản này thì {@code FrameworkVersionRepository.findActiveVersionId} trả rỗng và
 * mọi phiên luyện tập đều không vào được -- nên đây là dữ liệu bắt buộc, không phải demo.
 *
 * <p>Bản dựng ra đã ở trạng thái PUBLISHED và thoả đúng những điều kiện mà
 * {@code UpdateFrameworkVersionStatusUseCase} bắt buộc khi xuất bản: đủ và chỉ 5 tiêu chí
 * theo {@code FrameworkCriterionCode.ALLOWED_CODES}, mỗi tiêu chí có đủ 6 bậc, mỗi bậc
 * có ít nhất một dấu hiệu tích cực và một dấu hiệu tiêu cực. Ghi thẳng qua repository là
 * đường tắt vòng qua use case đó, nên các bất biến phải được giữ bằng tay ở đây.
 *
 * <p>Số bậc KHÔNG bị đóng cứng ở đâu khác trong hệ: thang bậc được suy từ
 * {@code result_band_order} (xem {@code LearnerProfileRepository.findFrameworkBandCount}),
 * nên mã bậc ở đây chỉ mang vai trò định danh, không mang ngữ nghĩa thứ tự.
 */
@Component
@Order(4)
public class FrameworkInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameworkInitializer.class);

    /**
     * Khung đánh giá mặc định là Khung năng lực ngoại ngữ 6 bậc dùng cho Việt Nam (KNLNNVN),
     * với 5 tiêu chí: Pronunciation, Fluency, Grammar, Vocabulary, Coherence.
     */
    private static final String FRAMEWORK_CODE = "KNLNNVN";

    // Mặc định khởi tạo là version 1
    private static final String FRAMEWORK_VERSION_CODE = "KNLNNVN_V1";

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

    // Sáu bậc của KNLNNVN. Bậc 1..6 tương ứng CEFR A1..C2 theo Thông tư 01/2014/TT-BGDĐT.
    //
    // Mã viết hoa liền, KHÔNG dấu cách: mã band là thứ đi ra ngoài hệ thống -- nó hiện trong
    // file Excel xuất điểm và màn hình kết quả, đồng thời là khoá đối chiếu khi import chính
    // sách chấm (AssessmentPolicyImportCommitHandler chuẩn hoá bằng toUpperCase). Dấu cách
    // trong mã sẽ theo vào cả ba đường đó. Phần chữ cho người đọc nằm ở label.
    private static final String FRAMEWORK_VERSION_BAND_CODE_1 = "BAC_1";
    private static final String FRAMEWORK_VERSION_BAND_CODE_2 = "BAC_2";
    private static final String FRAMEWORK_VERSION_BAND_CODE_3 = "BAC_3";
    private static final String FRAMEWORK_VERSION_BAND_CODE_4 = "BAC_4";
    private static final String FRAMEWORK_VERSION_BAND_CODE_5 = "BAC_5";
    private static final String FRAMEWORK_VERSION_BAND_CODE_6 = "BAC_6";

    /**
     * Số dấu hiệu đầu danh sách được đánh HIGH. Danh sách trong {@link BandContent} viết theo
     * thứ tự giảm dần độ quyết định, nên hai cái đầu là thứ một mình nó đã đủ xếp bậc, phần
     * còn lại là dấu hiệu hỗ trợ.
     *
     * <p>Lưu ý: {@code importance} KHÔNG đi ra tới model chấm -- cả hai đường gửi yêu cầu chấm
     * (SubmitExamSessionUseCase và PracticeEvaluationRequestFactory) chỉ lấy
     * {@code signal.description()}. Trường này phục vụ màn hình quản trị khung và chỗ dựa nếu
     * sau này prompt có xếp hạng dấu hiệu.
     */
    private static final int HIGH_IMPORTANCE_SIGNAL_COUNT = 2;

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
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_1, "Bậc 1",
            "Tương đương CEFR A1. Giao tiếp được ở mức từ và cụm từ rời rạc trong tình huống rất quen thuộc.", 1),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_2, "Bậc 2",
            "Tương đương CEFR A2. Trao đổi được thông tin đơn giản về chủ đề quen thuộc bằng câu ngắn.", 2),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_3, "Bậc 3",
            "Tương đương CEFR B1. Trình bày được trải nghiệm, kế hoạch và lý do ở mức đủ rõ để người nghe theo kịp.", 3),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_4, "Bậc 4",
            "Tương đương CEFR B2. Thảo luận trôi chảy nhiều chủ đề, bảo vệ được quan điểm bằng lập luận có cấu trúc.", 4),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_5, "Bậc 5",
            "Tương đương CEFR C1. Diễn đạt linh hoạt và chính xác, xử lý được cả chủ đề trừu tượng.", 5),
        new BandSeed(FRAMEWORK_VERSION_BAND_CODE_6, "Bậc 6",
            "Tương đương CEFR C2. Diễn đạt tự nhiên, chính xác và có sắc thái gần với người bản ngữ.", 6)
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
            "Khung năng lực ngoại ngữ 6 bậc dùng cho Việt Nam",
            "Khung đánh giá năng lực nói mặc định của hệ thống, gồm 5 tiêu chí và 6 bậc từ Bậc 1 đến Bậc 6 "
                + "(Thông tư 01/2014/TT-BGDĐT, tương đương CEFR A1-C2).",
            true,
            now, now, null, null
        ));

        // effectiveFrom = now và effectiveTo = null là điều kiện để findActiveVersionId nhìn
        // thấy bản này; thiếu effectiveFrom thì luyện tập vẫn không chạy dù đã PUBLISHED.
        var version = frameworkVersionRepository.save(new FrameworkVersion(
            framework.getId(),
            FRAMEWORK_VERSION_CODE,
            "KNLNNVN phiên bản 1",
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
                    signals(criterion.getCode() + "_" + resultBand.getCode() + "_POS", content.positive()),
                    signals(criterion.getCode() + "_" + resultBand.getCode() + "_NEG", content.negative()),
                    now, now, null, null
                ));
            }
        }
        return bands;
    }

    /**
     * Mã dấu hiệu được đánh số theo vị trí trong danh sách (…_POS_1, …_POS_2). Mã phải duy
     * nhất trong phạm vi một ô tiêu chí x bậc vì đó là thứ màn hình quản trị dùng để trỏ vào
     * đúng dấu hiệu khi sửa.
     */
    private FrameworkCriterionSignals signals(String codePrefix, List<String> descriptions) {
        var values = new ArrayList<FrameworkCriterionSignal>(descriptions.size());
        for (var index = 0; index < descriptions.size(); index++) {
            values.add(new FrameworkCriterionSignal(
                codePrefix + "_" + (index + 1),
                descriptions.get(index),
                index < HIGH_IMPORTANCE_SIGNAL_COUNT
                    ? FrameworkCriterionSignalImportance.HIGH
                    : FrameworkCriterionSignalImportance.MEDIUM,
                null));
        }
        return new FrameworkCriterionSignals(values);
    }

    private Map<String, Map<String, BandContent>> bandContent() {
        var content = new LinkedHashMap<String, Map<String, BandContent>>();
        content.put(FRAMEWORK_VERSION_CRITERION_CODE_PRONUNCIATION, pronunciationBands());
        content.put(FRAMEWORK_VERSION_CRITERION_CODE_FLUENCY, fluencyBands());
        content.put(FRAMEWORK_VERSION_CRITERION_CODE_GRAMMAR, grammarBands());
        content.put(FRAMEWORK_VERSION_CRITERION_CODE_VOCABULARY, vocabularyBands());
        content.put(FRAMEWORK_VERSION_CRITERION_CODE_COHERENCE, coherenceBands());
        return content;
    }

    private Map<String, BandContent> pronunciationBands() {
        return Map.of(
            FRAMEWORK_VERSION_BAND_CODE_1, new BandContent(
                "Phát âm ở mức từ rời rạc: người nghe chỉ nhận ra được những từ đã học thuộc và phải dựa "
                    + "nhiều vào ngữ cảnh hoặc câu hỏi gợi ý mới đoán ra ý. Âm cuối, cụm phụ âm và trọng âm "
                    + "gần như chưa được kiểm soát; nhiều từ bị phát âm theo hệ âm tiếng Việt -- thêm nguyên "
                    + "âm đệm vào cụm phụ âm, lược bỏ phụ âm cuối. Ngữ điệu phẳng đều nên không phân biệt "
                    + "được câu hỏi với câu kể. Người nghe phải yêu cầu nhắc lại nhiều lần ngay cả với nội "
                    + "dung rất quen thuộc.",
                List.of(
                    "Đọc rõ được các từ đơn quen thuộc đã học thuộc lòng (số đếm, màu sắc, tên gọi bản thân) khi đứng riêng lẻ.",
                    "Người nghe đã quen với lỗi phát âm của người Việt vẫn nhận ra được từ khoá chính trong phát ngôn.",
                    "Phát âm đúng các nguyên âm đơn cơ bản trong những từ ngắn một âm tiết.",
                    "Lặp lại được theo mẫu khi được sửa, dù chưa giữ được cách phát âm đúng ở lần nói sau."),
                List.of(
                    "Lược bỏ hoặc thay thế âm cuối ở hầu hết các từ (work → wɔ, like → lai), làm mất luôn thông tin về thì và số nhiều.",
                    "Người nghe phải hỏi lại nhiều lần hoặc tự đoán theo ngữ cảnh mới hiểu được ý.",
                    "Chèn thêm nguyên âm vào cụm phụ âm (school → sư-kun) khiến từ bị kéo dài thành nhiều âm tiết.",
                    "Đặt trọng âm sai ở phần lớn từ đa âm tiết, hoặc đọc đều tất cả các âm tiết như tiếng Việt.",
                    "Ngữ điệu phẳng suốt lượt nói, không phân biệt được câu hỏi với câu trần thuật.")),
            FRAMEWORK_VERSION_BAND_CODE_2, new BandContent(
                "Phát âm đủ để hiểu trong những câu ngắn về chủ đề quen thuộc, nhưng ảnh hưởng của tiếng mẹ "
                    + "đẻ còn rõ ở cả âm đoạn lẫn ngữ điệu. Người nghe nắm được ý chính mà không phải hỏi lại "
                    + "quá nhiều, song vẫn phải tập trung và đôi chỗ dựa vào ngữ cảnh để bù cho âm sai. Âm cuối "
                    + "và cụm phụ âm còn bị nuốt tới mức làm lệch nghĩa. Trọng âm từ đúng ở các từ thông dụng "
                    + "nhưng sai ở từ dài hoặc ít gặp. Nói nhanh hơn bình thường là mức dễ hiểu tụt xuống thấy rõ.",
                List.of(
                    "Phát âm đúng phần lớn từ thông dụng trong câu ngắn, đủ để người nghe nắm ý chính mà không cần nhắc lại.",
                    "Giữ được âm cuối ở những từ quen thuộc, nhất là khi nói chậm.",
                    "Đặt đúng trọng âm ở các từ hai âm tiết thông dụng.",
                    "Lên giọng cuối câu hỏi Yes/No một cách tương đối ổn định.",
                    "Ngắt nghỉ đúng chỗ ở câu ngắn, không cắt ngang giữa cụm từ."),
                List.of(
                    "Nuốt hoặc thay âm cuối ở mức làm lệch nghĩa của từ (wanted → want, cats → cat).",
                    "Nhầm lẫn có hệ thống ở các cặp âm gần nhau (/θ/–/t/, /s/–/ʃ/, /iː/–/ɪ/) khiến người nghe phải suy đoán.",
                    "Trọng âm sai ở từ ba âm tiết trở lên, làm từ trở nên khó nhận ra.",
                    "Ngữ điệu đơn điệu suốt cả lượt nói, không nhấn được từ mang thông tin mới.",
                    "Nói nhanh hơn nhịp quen thuộc là độ dễ hiểu tụt xuống rõ rệt.")),
            FRAMEWORK_VERSION_BAND_CODE_3, new BandContent(
                "Phát âm rõ ở phần lớn phát ngôn: người nghe quen tai theo được trọn ý mà không phải hỏi lại, "
                    + "kể cả khi chủ đề vượt ra ngoài vùng học thuộc. Lỗi vẫn còn nhưng đã thành cục bộ ở một số "
                    + "âm hoặc một nhóm từ cụ thể, không còn trải đều toàn bài. Trọng âm từ đúng ở đa số trường "
                    + "hợp; ngữ điệu đã có lên xuống nhưng chưa được dùng chủ động để nhấn ý. Chất lượng phát âm "
                    + "giảm nhẹ khi phải nói câu dài hoặc dùng từ mới gặp.",
                List.of(
                    "Người nghe theo được trọn ý mà không phải yêu cầu nhắc lại.",
                    "Đặt trọng âm từ chính xác ở đa số trường hợp, kể cả với một số từ ba âm tiết.",
                    "Giữ được âm cuối đủ để phân biệt thì và số nhiều trong phần lớn câu.",
                    "Ngắt nhịp theo cụm nghĩa chứ không ngắt tuỳ tiện giữa cụm từ.",
                    "Tự nhận ra và sửa lại một số từ vừa phát âm sai."),
                List.of(
                    "Ngữ điệu còn phẳng, một vài chỗ khiến người nghe phải hỏi lại hoặc xác nhận lại ý.",
                    "Lặp lại cùng một lỗi âm ở một nhóm từ nhất định trong suốt bài nói.",
                    "Cụm phụ âm khó (/str/, /kts/, /lθs/) vẫn bị giản lược.",
                    "Nói câu dài là độ rõ ràng giảm xuống thấy rõ so với câu ngắn.",
                    "Chưa nhấn được từ mang thông tin mới, khiến trọng tâm câu bị mờ.")),
            FRAMEWORK_VERSION_BAND_CODE_4, new BandContent(
                "Phát âm rõ ràng và ổn định trong cả bài nói dài; người nghe không phải bỏ công sức đáng kể "
                    + "để hiểu, kể cả với chủ đề không quen. Trọng âm từ, trọng âm câu và ngữ điệu đã đủ để làm "
                    + "nổi ý muốn nhấn và phân tách thông tin cũ với thông tin mới. Đã có nối âm và giản lược tự "
                    + "nhiên ở tốc độ nói bình thường. Lỗi còn lại chỉ lẻ tẻ, thường rơi vào cụm phụ âm khó hoặc "
                    + "từ ít gặp, và không gây hiểu sai. Ảnh hưởng tiếng mẹ đẻ còn nghe được nhưng không cản trở.",
                List.of(
                    "Dùng ngữ điệu để làm nổi bật thông tin quan trọng và đánh dấu chỗ chuyển ý.",
                    "Người nghe không phải tập trung bất thường để theo được nội dung.",
                    "Nối âm và giản lược tự nhiên ở tốc độ nói bình thường (want to → wanna, in an hour).",
                    "Giữ chất lượng phát âm ổn định từ đầu đến cuối lượt nói dài.",
                    "Phát âm đúng cả từ mới gặp lần đầu nhờ nắm được quy tắc chung."),
                List.of(
                    "Còn lỗi lẻ tẻ ở các cụm phụ âm khó hoặc từ chuyên ngành ít gặp.",
                    "Trọng âm câu đôi chỗ đặt sai chỗ, làm trọng tâm thông tin bị lệch nhẹ.",
                    "Ảnh hưởng của tiếng mẹ đẻ vẫn nhận ra được, dù không gây hiểu sai.",
                    "Khi nói nhanh hoặc bị cuốn theo cảm xúc, một vài âm cuối bị lướt.")),
            FRAMEWORK_VERSION_BAND_CODE_5, new BandContent(
                "Phát âm tự nhiên và chính xác ở mọi độ dài phát ngôn; người nghe hoàn toàn không phải bỏ "
                    + "công sức để giải mã. Điều chỉnh được nhịp, trọng âm câu và ngữ điệu theo sắc thái muốn "
                    + "truyền đạt -- nhấn mạnh, đối lập, dè dặt. Dùng thành thạo các hiện tượng nối âm, đồng hoá "
                    + "và giản lược của lời nói liên tục. Dấu vết tiếng mẹ đẻ chỉ còn thoáng qua ở từ hiếm gặp "
                    + "và không tạo ra hiểu nhầm nào.",
                List.of(
                    "Thay đổi nhịp và trọng âm câu linh hoạt theo ý đồ diễn đạt (nhấn mạnh, đối lập, dè dặt).",
                    "Giữ được độ rõ ràng ngay cả khi nói nhanh hoặc khi diễn đạt ý phức tạp.",
                    "Dùng đúng các hiện tượng nối âm, đồng hoá và giản lược trong lời nói liên tục.",
                    "Ngữ điệu phù hợp với thái độ và mức trang trọng của tình huống.",
                    "Phát âm chính xác cả thuật ngữ chuyên ngành và tên riêng ít gặp."),
                List.of(
                    "Thỉnh thoảng lộ dấu vết tiếng mẹ đẻ ở từ hiếm gặp hoặc tên riêng nước ngoài.",
                    "Một vài chỗ ngữ điệu chưa khớp hoàn toàn với sắc thái định truyền đạt.",
                    "Ở phát ngôn dài và phức tạp, đôi khi phải nói chậm lại một chút để giữ độ rõ.")),
            FRAMEWORK_VERSION_BAND_CODE_6, new BandContent(
                "Phát âm chính xác, tự nhiên và nhất quán như người dùng ngôn ngữ thành thục; không tạo bất "
                    + "kỳ tải nghe nào cho người đối thoại trong bất kỳ tình huống nào. Kiểm soát ngữ điệu ở mức "
                    + "tinh tế: truyền được cả hàm ý, thái độ và sắc thái mà từ ngữ không nói ra. Điều chỉnh được "
                    + "cách phát âm theo đối tượng nghe và mức trang trọng. Không còn lỗi nào có hệ thống; các "
                    + "biến thể phát âm nếu có đều là lựa chọn có ý thức chứ không phải hạn chế.",
                List.of(
                    "Kiểm soát ngữ điệu tinh tế để truyền được cả hàm ý, thái độ và sắc thái ngoài lời.",
                    "Duy trì phát âm chuẩn xác và ổn định trong mọi tình huống, kể cả khi ứng biến hoặc bị ngắt lời.",
                    "Điều chỉnh được cách phát âm và nhịp nói theo đối tượng nghe và mức trang trọng.",
                    "Các biến thể phát âm xuất hiện đều là lựa chọn có chủ đích, không phải do hạn chế."),
                List.of(
                    "Nếu còn bất kỳ lỗi âm có hệ thống nào lặp lại, chưa đạt bậc này.",
                    "Nếu người nghe phải tập trung bất thường ở dù chỉ một vài chỗ, chưa đạt bậc này.",
                    "Nếu độ rõ ràng giảm khi chuyển sang chủ đề trừu tượng hoặc khi nói nhanh, chỉ nên xếp bậc dưới.",
                    "Nếu ngữ điệu chỉ đúng về hình thức mà không truyền được sắc thái, chỉ nên xếp bậc dưới."))
        );
    }

    private Map<String, BandContent> fluencyBands() {
        return Map.of(
            FRAMEWORK_VERSION_BAND_CODE_1, new BandContent(
                "Lời nói bị ngắt quãng liên tục: chủ yếu là từ đơn hoặc cụm hai, ba từ xen giữa những khoảng "
                    + "lặng dài. Mỗi phát ngôn đều cần thời gian chuẩn bị đáng kể, và người nghe thường phải chờ, "
                    + "nhắc hoặc đặt câu hỏi phụ thì lượt nói mới tiếp tục. Chưa có chiến lược nào để giữ lượt nói "
                    + "trong lúc đang nghĩ. Tổng thời lượng nói thực tế chiếm phần nhỏ so với thời gian được cấp "
                    + "cho câu hỏi.",
                List.of(
                    "Bật ra được từ cần dùng sau khi có thời gian nghĩ.",
                    "Nói được cụm hai đến ba từ liền mạch trong tình huống rất quen thuộc.",
                    "Cố gắng đáp lại thay vì im lặng hoàn toàn khi được hỏi."),
                List.of(
                    "Khoảng lặng dài (trên ba giây) xuất hiện liên tục, làm đứt mạch giao tiếp.",
                    "Người nghe phải nhắc hoặc đặt câu hỏi phụ thì lượt nói mới tiếp tục được.",
                    "Thời lượng nói thực tế chỉ chiếm phần nhỏ so với thời gian được cấp cho câu hỏi.",
                    "Lặp đi lặp lại một từ trong lúc tìm từ tiếp theo, không có chiến lược giữ lượt nói.",
                    "Bỏ dở phát ngôn giữa chừng rồi im lặng thay vì tìm cách nói khác.")),
            FRAMEWORK_VERSION_BAND_CODE_2, new BandContent(
                "Nói được thành câu ngắn nhưng ngập ngừng rõ mỗi khi phải tìm từ hoặc dựng cấu trúc. Duy trì "
                    + "được lượt nói về chủ đề quen thuộc, còn chủ đề mới thì nhịp chậm hẳn lại hoặc dừng. Việc "
                    + "lặp lại và tự sửa chiếm tỷ lệ đáng kể trong lời nói, khiến người nghe phải kiên nhẫn. Đã "
                    + "có vài từ đệm quen thuộc nhưng dùng máy móc. Ngập ngừng còn rơi vào giữa cụm từ, cho thấy "
                    + "câu đang được dựng từng chữ một.",
                List.of(
                    "Duy trì được lượt nói ngắn về chủ đề quen thuộc mà không cần người nghe can thiệp.",
                    "Nói được vài câu liên tiếp khi nội dung nằm trong phần đã chuẩn bị.",
                    "Dùng được một vài từ đệm (well, you know) để giữ lượt nói.",
                    "Đáp lại câu hỏi trong khoảng thời gian chấp nhận được, không để trống quá lâu."),
                List.of(
                    "Lặp lại và tự sửa nhiều tới mức làm chậm hẳn nhịp nói.",
                    "Ngập ngừng ngay giữa cụm từ chứ không chỉ ở chỗ chuyển ý, cho thấy đang dựng câu từng chữ.",
                    "Chủ đề ra ngoài phần quen thuộc là nhịp nói tụt xuống thấy rõ hoặc dừng hẳn.",
                    "Dùng từ đệm một cách máy móc, lặp cùng một từ đệm nhiều lần liên tiếp.",
                    "Không nói hết thời lượng được cấp dù còn ý chưa trình bày.")),
            FRAMEWORK_VERSION_BAND_CODE_3, new BandContent(
                "Nói liên tục được những đoạn dài vừa phải; ngập ngừng đã dồn về chỗ chuyển ý thay vì rải đều "
                    + "trong câu. Người nghe theo được mà không phải chờ hay nhắc. Tốc độ nói còn chậm hơn nhịp "
                    + "hội thoại tự nhiên và chậm lại rõ khi gặp chủ đề ngoài vùng quen thuộc, nhưng mạch nói "
                    + "không bị đứt. Đã dùng được một vài chiến lược câu giờ để giữ lượt nói trong lúc tìm cách "
                    + "diễn đạt.",
                List.of(
                    "Giữ được mạch nói mà không cần người nghe nhắc hay gợi ý.",
                    "Ngập ngừng chủ yếu rơi vào chỗ chuyển ý, không cắt ngang giữa cụm từ.",
                    "Nói đủ dài để trả lời trọn vẹn câu hỏi trong thời lượng được cấp.",
                    "Dùng cụm câu giờ tự nhiên (let me think, that's a good question) để giữ lượt nói.",
                    "Sau khi ngập ngừng vẫn quay lại đúng ý đang nói dở."),
                List.of(
                    "Nhịp chậm lại rõ rệt khi gặp chủ đề ngoài vùng quen thuộc.",
                    "Còn những quãng dừng đủ dài để người nghe nhận ra là đang bí từ.",
                    "Tốc độ nói tổng thể còn thấp hơn nhịp hội thoại tự nhiên.",
                    "Tự sửa làm gãy câu ở một vài chỗ, phải bắt đầu lại câu từ đầu.",
                    "Về cuối lượt nói dài có dấu hiệu đuối nhịp so với phần đầu.")),
            FRAMEWORK_VERSION_BAND_CODE_4, new BandContent(
                "Nói trôi chảy ở nhịp gần với hội thoại tự nhiên và duy trì được trong cả đoạn dài. Ngập ngừng "
                    + "vẫn có nhưng không cản trở người nghe, và thường là để chọn cách diễn đạt chứ không phải "
                    + "để tìm từ cơ bản. Tự sửa gọn và nhanh, không làm gãy mạch trình bày. Xử lý được cả những "
                    + "câu hỏi bất ngờ mà không tụt nhịp đáng kể.",
                List.of(
                    "Duy trì nhịp nói ổn định trong suốt đoạn dài, không đuối về cuối.",
                    "Tự sửa gọn và nhanh, không làm gãy mạch trình bày.",
                    "Trả lời được câu hỏi bất ngờ mà không tụt nhịp đáng kể.",
                    "Ngập ngừng chủ yếu để chọn cách diễn đạt tốt hơn, không phải để tìm từ cơ bản."),
                List.of(
                    "Còn vài chỗ dừng lại để tìm cấu trúc phù hợp, nhất là với ý phức tạp.",
                    "Nhịp nói giảm nhẹ khi phải triển khai lập luận nhiều tầng.",
                    "Đôi chỗ dùng cách diễn đạt vòng vo để tránh cấu trúc chưa chắc chắn.")),
            FRAMEWORK_VERSION_BAND_CODE_5, new BandContent(
                "Nói trôi chảy, đều nhịp và gần như không cần nỗ lực, kể cả khi triển khai lập luận dài hoặc "
                    + "bàn về chủ đề trừu tượng. Quãng nghỉ xuất hiện đúng chỗ và mang chức năng tu từ -- nhấn ý, "
                    + "tạo tương phản, nhường lượt -- chứ không phải dấu hiệu bí từ. Rất hiếm khi phải dừng để "
                    + "tìm cách diễn đạt, và nếu có thì người nghe khó nhận ra.",
                List.of(
                    "Chủ động dùng quãng nghỉ như một công cụ nhấn ý chứ không phải để tìm từ.",
                    "Triển khai được lập luận dài nhiều tầng mà nhịp nói vẫn đều.",
                    "Chuyển giữa các chủ đề, kể cả chủ đề trừu tượng, mà không tụt nhịp.",
                    "Ứng biến trơn tru khi bị hỏi ngược hoặc bị ngắt lời."),
                List.of(
                    "Vẫn còn một vài lần phải dừng lại tìm cách diễn đạt, dù rất thưa.",
                    "Ở chủ đề chuyên sâu ngoài lĩnh vực quen, nhịp nói chậm lại một chút.",
                    "Đôi khi câu bị kéo dài hơn cần thiết trong lúc điều chỉnh ý giữa chừng.")),
            FRAMEWORK_VERSION_BAND_CODE_6, new BandContent(
                "Nói tự nhiên và liền mạch như trong hội thoại đời thường, không có bất kỳ dấu hiệu nào của "
                    + "việc phải xoay xở với ngôn ngữ. Nhịp nói được điều tiết hoàn toàn theo nội dung và mục "
                    + "đích giao tiếp: nhanh khi kể, chậm lại khi giải thích, dừng đúng chỗ khi muốn người nghe "
                    + "ngấm ý. Ngập ngừng nếu có đều là ngập ngừng do đang cân nhắc nội dung, giống hệt người "
                    + "dùng ngôn ngữ thành thục.",
                List.of(
                    "Điều tiết nhịp nói theo nội dung một cách có chủ đích: nhanh khi kể, chậm khi giải thích.",
                    "Ngập ngừng nếu có là do cân nhắc nội dung, không phải do thiếu phương tiện diễn đạt.",
                    "Duy trì độ trôi chảy hoàn toàn ổn định ở mọi chủ đề và mọi độ dài phát ngôn.",
                    "Ứng biến tức thì trong tình huống bất ngờ mà không mất nhịp."),
                List.of(
                    "Nếu còn bất kỳ ngập ngừng nào do thiếu phương tiện diễn đạt, chưa đạt bậc này.",
                    "Nếu độ trôi chảy đạt được bằng cách né tránh ý khó, chưa đạt bậc này.",
                    "Nếu nhịp nói chậm lại khi chuyển sang chủ đề trừu tượng hoặc chuyên sâu, chỉ nên xếp bậc dưới.",
                    "Nếu về cuối lượt nói dài có dấu hiệu đuối nhịp, chỉ nên xếp bậc dưới."))
        );
    }

    private Map<String, BandContent> grammarBands() {
        return Map.of(
            FRAMEWORK_VERSION_BAND_CODE_1, new BandContent(
                "Chủ yếu dựa vào cụm từ và mẫu câu học thuộc; cấu trúc câu chưa hình thành rõ nên nhiều phát "
                    + "ngôn chỉ là chuỗi từ ghép lại. Sai ở những điểm cơ bản nhất -- thì, số ít/số nhiều, động "
                    + "từ to be, trật tự từ -- với mật độ dày tới mức người nghe phải dựa vào ngữ cảnh mới hiểu. "
                    + "Chưa dựng được câu ghép; các ý được đặt cạnh nhau mà không có liên kết ngữ pháp nào.",
                List.of(
                    "Ghép được chủ ngữ và động từ trong những mẫu câu quen thuộc đã học thuộc.",
                    "Dùng đúng một vài mẫu câu cố định (I am..., I like..., This is...).",
                    "Trật tự từ đúng trong các câu ngắn nhất."),
                List.of(
                    "Sai cơ bản ở thì và số ít/số nhiều với mật độ dày, gây khó hiểu.",
                    "Thiếu hoặc thừa động từ to be trong phần lớn câu (I student, I am go).",
                    "Trật tự từ bị áp theo tiếng Việt, nhất là ở cụm danh từ và trạng ngữ.",
                    "Chưa dựng được câu ghép; các ý đặt cạnh nhau không có liên kết ngữ pháp.",
                    "Bỏ dở cấu trúc đang nói giữa chừng vì không dựng tiếp được.")),
            FRAMEWORK_VERSION_BAND_CODE_2, new BandContent(
                "Dùng đúng được một số cấu trúc đơn giản trong ngữ cảnh quen thuộc, chủ yếu là câu đơn ở thì "
                    + "hiện tại đơn và quá khứ đơn. Bước ra khỏi những mẫu đã học là mật độ lỗi tăng lên rõ. Câu "
                    + "ghép mới ở mức thử nghiệm và thường sai trật tự hoặc thiếu thành phần. Lỗi còn nhiều nhưng "
                    + "đã bắt đầu có quy luật, cho thấy đang áp dụng quy tắc chứ không nói theo trí nhớ.",
                List.of(
                    "Dùng đúng thì hiện tại đơn và quá khứ đơn ở câu ngắn quen thuộc.",
                    "Dựng đúng câu phủ định và câu hỏi cơ bản với do/does/did.",
                    "Dùng đúng một số giới từ chỉ thời gian và nơi chốn thông dụng.",
                    "Lỗi đã có quy luật, cho thấy đang áp dụng quy tắc chứ không nói theo trí nhớ."),
                List.of(
                    "Câu ghép thường sai trật tự hoặc thiếu thành phần khi nối hai mệnh đề.",
                    "Chia sai động từ ngôi thứ ba số ít một cách hệ thống.",
                    "Dùng sai hoặc bỏ mạo từ ở phần lớn cụm danh từ.",
                    "Ra khỏi nhóm mẫu câu đã học là mật độ lỗi tăng lên rõ rệt.",
                    "Chỉ dùng được câu đơn; mọi ý phức tạp đều bị cắt thành các câu rời.")),
            FRAMEWORK_VERSION_BAND_CODE_3, new BandContent(
                "Dùng được nhiều cấu trúc thông dụng, gồm cả câu ghép và một số mệnh đề phụ. Lỗi vẫn còn nhưng "
                    + "đã tập trung vào một vài điểm ngữ pháp nhất định và phần lớn không cản trở việc hiểu. Ở câu "
                    + "đơn về chủ đề quen thuộc, độ chính xác khá ổn định; ở câu dài hoặc cấu trúc mới, lỗi tăng "
                    + "lên. Đã dùng được thì hoàn thành và một số dạng bị động cơ bản, dù chưa ổn định.",
                List.of(
                    "Kết hợp được mệnh đề phụ vào câu một cách hợp lý (because, when, if, that).",
                    "Lỗi phần lớn không cản trở người nghe nắm ý.",
                    "Giữ độ chính xác khá ổn định ở câu đơn về chủ đề quen thuộc.",
                    "Dùng được thì hoàn thành và câu bị động cơ bản, dù chưa hoàn toàn ổn định.",
                    "Tự nhận ra một số lỗi vừa mắc và sửa lại được."),
                List.of(
                    "Sai lặp ở một vài điểm ngữ pháp nhất định trong suốt bài nói.",
                    "Cấu trúc phức tạp hơn (câu điều kiện loại 2, mệnh đề quan hệ) thường sai hoặc bị né tránh.",
                    "Phối hợp thì giữa các mệnh đề chưa nhất quán khi kể một chuỗi sự việc.",
                    "Câu càng dài thì lỗi càng dày, đôi khi bỏ dở cấu trúc đang dựng.",
                    "Dùng lặp một vài kiểu câu quen thuộc cho hầu hết nội dung.")),
            FRAMEWORK_VERSION_BAND_CODE_4, new BandContent(
                "Kiểm soát ngữ pháp tốt và ổn định: lỗi thưa, hiếm khi gây hiểu sai, và người nói thường tự "
                    + "nhận ra. Dùng đa dạng cấu trúc -- mệnh đề quan hệ, câu điều kiện, bị động, cụm phân từ -- "
                    + "phù hợp với ý muốn diễn đạt chứ không phải để phô cấu trúc. Ở câu dài nhiều tầng vẫn giữ "
                    + "được sự nhất quán về thì và tham chiếu, chỉ tụt nhẹ khi nói nhanh hoặc phải ứng biến.",
                List.of(
                    "Dùng đa dạng cấu trúc phù hợp với ý muốn diễn đạt, không phải để phô cấu trúc.",
                    "Lỗi thưa và hầu như không gây hiểu sai.",
                    "Giữ được sự nhất quán về thì và tham chiếu trong cả đoạn dài.",
                    "Dựng được câu phức nhiều tầng mà vẫn hoàn chỉnh về cấu trúc."),
                List.of(
                    "Còn lỗi ở cấu trúc phức tạp khi nói nhanh hoặc khi ứng biến.",
                    "Một vài cấu trúc nâng cao (đảo ngữ, thức giả định) dùng chưa thật tự nhiên.",
                    "Đôi chỗ lặp lại cùng một kiểu câu nhiều lần liên tiếp.")),
            FRAMEWORK_VERSION_BAND_CODE_5, new BandContent(
                "Ngữ pháp chính xác và linh hoạt ở mọi độ phức tạp, kể cả câu dài nhiều tầng và ý trừu tượng. "
                    + "Cấu trúc được chọn để phục vụ sắc thái -- nhấn mạnh, giảm nhẹ, dè dặt, tương phản -- chứ "
                    + "không chỉ để đúng. Điều chỉnh được độ phức tạp cú pháp theo mức trang trọng của tình huống. "
                    + "Lỗi chỉ còn lẻ tẻ, thường là lỡ lời, và được tự nhận ra rồi sửa ngay mà không làm gãy mạch.",
                List.of(
                    "Chọn cấu trúc phục vụ sắc thái chứ không chỉ để đúng (đảo ngữ nhấn mạnh, giả định, giảm nhẹ).",
                    "Giữ độ chính xác cao ngay cả ở câu dài nhiều tầng và ý trừu tượng.",
                    "Lỗi lẻ tẻ, tự nhận ra và sửa ngay mà không làm gãy mạch.",
                    "Điều chỉnh độ phức tạp cú pháp theo mức trang trọng của tình huống."),
                List.of(
                    "Vẫn còn vài lỗi lỡ lời ở cấu trúc hiếm gặp, dù tự sửa được.",
                    "Đôi chỗ câu phức bị kéo dài quá mức cần thiết, làm ý bị loãng.",
                    "Ở chủ đề chuyên sâu ngoài lĩnh vực quen, độ đa dạng cấu trúc giảm nhẹ.")),
            FRAMEWORK_VERSION_BAND_CODE_6, new BandContent(
                "Ngữ pháp chuẩn xác một cách nhất quán ở mọi độ phức tạp và mọi tình huống, kể cả khi nói tự "
                    + "phát hoặc bị ngắt lời. Vận dụng cấu trúc một cách tinh tế để tạo hiệu quả diễn đạt, và mỗi "
                    + "lựa chọn cú pháp đều có lý do. Nếu có lỡ lời thì đó là kiểu lỡ lời của người dùng ngôn ngữ "
                    + "thành thục -- nói nhầm rồi sửa ngay -- chứ không phải lỗi do chưa nắm quy tắc.",
                List.of(
                    "Vận dụng cấu trúc tinh tế để tạo hiệu quả diễn đạt, mỗi lựa chọn cú pháp đều có lý do.",
                    "Gần như không có lỗi ngay cả khi nói tự phát hoặc bị ngắt lời.",
                    "Kiểm soát hoàn toàn các cấu trúc phức tạp và ít gặp.",
                    "Điều chỉnh văn phong cú pháp theo đối tượng nghe một cách chính xác."),
                List.of(
                    "Nếu còn bất kỳ lỗi ngữ pháp lặp lại có hệ thống nào, chưa đạt bậc này.",
                    "Nếu lỗi xuất hiện là do chưa nắm quy tắc chứ không phải lỡ lời, chưa đạt bậc này.",
                    "Nếu độ chính xác giảm khi tăng độ phức tạp của ý, chỉ nên xếp bậc dưới.",
                    "Nếu độ đa dạng cấu trúc đạt được bằng cách né tránh ý khó, chỉ nên xếp bậc dưới."))
        );
    }

    private Map<String, BandContent> vocabularyBands() {
        return Map.of(
            FRAMEWORK_VERSION_BAND_CODE_1, new BandContent(
                "Vốn từ rất hạn chế, chỉ đủ cho nhu cầu cơ bản nhất về bản thân và môi trường gần gũi. Chủ yếu "
                    + "là từ đơn và cụm cố định đã học thuộc; chưa có khả năng thay thế khi không nhớ ra từ. Việc "
                    + "thiếu từ thường xuyên khiến người nói phải bỏ dở ý, chuyển sang tiếng Việt hoặc dùng cử chỉ. "
                    + "Chưa phân biệt được các từ gần nghĩa.",
                List.of(
                    "Dùng đúng một số từ về bản thân và môi trường quanh mình (gia đình, trường lớp, sở thích).",
                    "Nhớ và dùng đúng một vài cụm cố định cơ bản trong tình huống giao tiếp quen.",
                    "Dùng được các từ chỉ số lượng, màu sắc và thời gian ở mức đơn giản nhất."),
                List.of(
                    "Thiếu từ tới mức phải bỏ dở ý đang nói.",
                    "Chuyển sang tiếng Việt hoặc dùng cử chỉ khi không nhớ ra từ.",
                    "Lặp lại một nhóm rất nhỏ các từ trong suốt bài nói.",
                    "Dùng sai từ tới mức làm người nghe hiểu sang nghĩa khác.",
                    "Không có cách nào diễn giải thay khi thiếu từ chính xác.")),
            FRAMEWORK_VERSION_BAND_CODE_2, new BandContent(
                "Đủ từ cho các chủ đề quen thuộc hằng ngày, nhưng còn lặp và phải diễn giải vòng vo khi ý vượt "
                    + "khỏi phạm vi đã học. Chủ yếu là từ thông dụng, chưa phân biệt được sắc thái giữa các từ gần "
                    + "nghĩa nên hay chọn nhầm cặp từ dễ lẫn. Đã dùng được một số cụm từ cố định nhưng còn máy móc. "
                    + "Chủ đề ngoài phạm vi đã học là phải né tránh hoặc rút gọn ý.",
                List.of(
                    "Dùng đúng từ thông dụng theo các chủ đề đã học (công việc, sở thích, thời tiết, đi lại).",
                    "Nói được về trải nghiệm cá nhân đơn giản mà không phải bỏ dở ý.",
                    "Bắt đầu thay được một vài từ gần nghĩa để tránh lặp.",
                    "Dùng đúng một số cụm từ cố định thông dụng trong ngữ cảnh phù hợp."),
                List.of(
                    "Lặp lại một nhóm từ nhỏ do không có lựa chọn thay thế.",
                    "Diễn giải vòng vo dài dòng thay cho một từ chính xác.",
                    "Chọn sai từ khi hai từ gần nghĩa nhưng khác ngữ cảnh (say/tell, make/do).",
                    "Chủ đề ngoài phạm vi đã học là phải né tránh hoặc rút gọn ý.",
                    "Chưa phân biệt được từ trang trọng với từ thông tục.")),
            FRAMEWORK_VERSION_BAND_CODE_3, new BandContent(
                "Đủ từ để nói trọn ý về các chủ đề quen thuộc mà không phải né tránh, và xoay xở được với chủ "
                    + "đề mới bằng cách diễn giải. Từ vựng còn thiên về nhóm thông dụng; độ chuẩn xác giảm khi ngữ "
                    + "cảnh đòi hỏi từ chuyên biệt hoặc sắc thái riêng. Đã dùng được một số cụm từ cố định và "
                    + "collocation thông dụng, dù còn sai ở mức nghe được là không tự nhiên.",
                List.of(
                    "Diễn giải được ý khi chưa nhớ ra từ chính xác, thay vì bỏ dở.",
                    "Nói trọn ý về chủ đề quen thuộc mà không phải né tránh nội dung.",
                    "Dùng được một số cụm từ cố định và collocation thông dụng.",
                    "Có ý thức chọn từ khác nhau để tránh lặp trong cùng một đoạn.",
                    "Hiểu và dùng lại được từ mới mà người đối thoại vừa đưa ra."),
                List.of(
                    "Chọn từ chưa chuẩn xác ở chủ đề ngoài vùng quen thuộc.",
                    "Collocation sai ở mức nghe được là không tự nhiên (do a mistake, make homework).",
                    "Từ vựng còn thiên về nhóm thông dụng, thiếu từ mang sắc thái riêng.",
                    "Khi cần nói chính xác về chi tiết, phải quay lại diễn giải dài dòng.",
                    "Dùng từ trung tính cho cả những chỗ ngữ cảnh đòi hỏi từ mạnh hoặc từ nhẹ hơn.")),
            FRAMEWORK_VERSION_BAND_CODE_4, new BandContent(
                "Vốn từ rộng, đủ để chọn từ phù hợp cho phần lớn ngữ cảnh, kể cả những chủ đề không quen. Dùng "
                    + "được cụm từ cố định, collocation và từ nối một cách tự nhiên. Sai sót còn lại chủ yếu là "
                    + "chưa khớp sắc thái hoặc mức trang trọng, không phải thiếu từ. Diễn giải vòng chỉ còn xuất "
                    + "hiện với thuật ngữ chuyên ngành hẹp.",
                List.of(
                    "Chọn được từ phù hợp cho cả chủ đề không quen mà không phải bỏ ý.",
                    "Dùng được cụm từ cố định, collocation và từ nối một cách tự nhiên.",
                    "Phân biệt được mức trang trọng của từ trong phần lớn trường hợp.",
                    "Nói về chủ đề trừu tượng vừa phải mà vốn từ vẫn đủ."),
                List.of(
                    "Đôi chỗ dùng từ chưa khớp sắc thái hoặc mức trang trọng của ngữ cảnh.",
                    "Còn phải diễn giải vòng với thuật ngữ chuyên ngành hẹp.",
                    "Thành ngữ và cách nói ẩn dụ dùng còn dè dặt hoặc đôi khi lệch ngữ cảnh.",
                    "Một vài từ được lặp lại nhiều hơn mức tự nhiên trong đoạn dài.")),
            FRAMEWORK_VERSION_BAND_CODE_5, new BandContent(
                "Vốn từ phong phú và chính xác, đủ để diễn đạt cả những ý trừu tượng và tinh tế mà không phải "
                    + "vòng vo. Chọn được từ đúng sắc thái, đúng mức trang trọng và đúng ngữ vực cho từng tình "
                    + "huống. Dùng thành ngữ, cách nói ẩn dụ và collocation ít gặp một cách tự nhiên, đúng chỗ. "
                    + "Hiếm khi lộ ra giới hạn về vốn từ.",
                List.of(
                    "Chọn từ chính xác kể cả ở chủ đề trừu tượng hoặc chuyên ngành.",
                    "Dùng thành ngữ và cách nói ẩn dụ đúng chỗ, không gượng ép.",
                    "Phân biệt và dùng đúng sắc thái giữa các từ gần nghĩa.",
                    "Điều chỉnh ngữ vực từ vựng theo tình huống và đối tượng nghe."),
                List.of(
                    "Vẫn còn một vài lần phải diễn giải vòng vì thiếu từ, dù rất thưa.",
                    "Ở lĩnh vực chuyên sâu hoàn toàn xa lạ, độ chính xác trong chọn từ giảm nhẹ.",
                    "Đôi khi dùng từ trang trọng hơn mức tình huống đòi hỏi.")),
            FRAMEWORK_VERSION_BAND_CODE_6, new BandContent(
                "Vốn từ rất rộng và chính xác, phân biệt được cả những sắc thái tinh tế giữa các từ gần nghĩa. "
                    + "Chọn từ có chủ đích để tạo hiệu quả riêng cho người nghe, kể cả khi chơi chữ hoặc dùng hàm "
                    + "ý. Kiểm soát tốt thành ngữ, cách nói thông tục và ngữ vực học thuật, chuyển đổi giữa chúng "
                    + "một cách chính xác. Không còn dấu hiệu nào của việc bị vốn từ giới hạn.",
                List.of(
                    "Chọn từ có chủ đích để tạo hiệu quả riêng cho người nghe, kể cả chơi chữ và hàm ý.",
                    "Kiểm soát chính xác thành ngữ, cách nói thông tục và ngữ vực học thuật.",
                    "Phân biệt được cả những sắc thái tinh tế giữa các từ gần nghĩa.",
                    "Diễn đạt được ý trừu tượng phức tạp mà không cần vòng vo."),
                List.of(
                    "Nếu có bất kỳ dấu hiệu nào của việc bị vốn từ giới hạn, chưa đạt bậc này.",
                    "Nếu độ chính xác chọn từ giảm khi chuyển sang lĩnh vực xa lạ, chưa đạt bậc này.",
                    "Nếu thành ngữ hoặc cách nói ẩn dụ dùng lệch ngữ cảnh, chỉ nên xếp bậc dưới.",
                    "Nếu vốn từ rộng nhưng ngữ vực không khớp tình huống, chỉ nên xếp bậc dưới."))
        );
    }

    private Map<String, BandContent> coherenceBands() {
        return Map.of(
            FRAMEWORK_VERSION_BAND_CODE_1, new BandContent(
                "Các ý được nói ra rời rạc, chưa có liên kết ngữ nghĩa hay hình thức giữa các câu; người nghe "
                    + "phải tự ghép các mảnh thông tin lại với nhau. Chưa có mở đầu hay kết thúc, bài nói dừng "
                    + "đột ngột khi hết ý. Thứ tự các ý chủ yếu theo thứ tự nghĩ ra chứ không theo một logic nào. "
                    + "Chưa dùng được từ nối ngoài một vài từ đơn giản nhất.",
                List.of(
                    "Sắp xếp được hai ý theo một trình tự dễ hiểu (trước - sau, nguyên nhân - kết quả).",
                    "Trả lời đúng vào nội dung câu hỏi, dù chưa triển khai được.",
                    "Dùng được từ nối 'and' để nối hai thông tin."),
                List.of(
                    "Không dùng từ nối, người nghe phải tự ghép ý lại với nhau.",
                    "Các câu đứng cạnh nhau mà không có quan hệ rõ ràng.",
                    "Không có mở đầu hay kết thúc; bài nói dừng đột ngột khi hết ý.",
                    "Thứ tự các ý theo thứ tự nghĩ ra, đôi khi quay lại ý đã nói mà không đánh dấu.")),
            FRAMEWORK_VERSION_BAND_CODE_2, new BandContent(
                "Nối được các câu bằng một vài liên từ cơ bản, đủ để người nghe thấy được quan hệ đơn giản giữa "
                    + "hai ý. Mạch ý giữ được trong vài câu rồi đứt khi đoạn nói dài hơn. Bố cục còn ở dạng liệt "
                    + "kê; chưa phân biệt được ý chính với ý phụ. Cùng một liên từ được dùng cho mọi quan hệ giữa "
                    + "các ý.",
                List.of(
                    "Dùng đúng các liên từ cơ bản như and, but, because, then.",
                    "Giữ được mạch ý trong một đoạn ngắn vài câu.",
                    "Trả lời theo trình tự thời gian một cách dễ theo dõi.",
                    "Có ý thức đưa ra lý do cho ý kiến của mình, dù ngắn."),
                List.of(
                    "Mạch ý đứt quãng khi đoạn nói dài hơn vài câu.",
                    "Bố cục ở dạng liệt kê, chưa phân biệt được ý chính với ý phụ.",
                    "Lặp lại cùng một liên từ cho mọi quan hệ giữa các ý.",
                    "Quay lại ý đã nói mà không đánh dấu, làm người nghe mất mạch.",
                    "Chưa có câu mở đầu hoặc câu chốt cho cả lượt nói.")),
            FRAMEWORK_VERSION_BAND_CODE_3, new BandContent(
                "Trình bày theo một trình tự rõ ràng; người nghe theo được mạch chính từ đầu đến cuối. Đã phân "
                    + "biệt được ý chính với ví dụ minh hoạ và biết dẫn dắt từ ý này sang ý khác. Chuyển ý đôi chỗ "
                    + "còn đột ngột và bố cục thiên về tuyến tính; chưa có kỹ thuật tổ chức nào phức tạp hơn liệt "
                    + "kê theo trình tự.",
                List.of(
                    "Dẫn dắt từ ý này sang ý khác một cách hợp lý.",
                    "Phân biệt được ý chính với ví dụ hoặc chi tiết minh hoạ.",
                    "Dùng được các từ nối chỉ trình tự và tương phản (first, then, however, for example).",
                    "Có câu mở đầu và câu chốt đủ để bài nói không bị cụt.",
                    "Giữ được chủ đề xuyên suốt, không lạc sang nội dung khác."),
                List.of(
                    "Chuyển ý còn đột ngột ở một vài chỗ, người nghe phải tự bắc cầu.",
                    "Bố cục thiên về liệt kê tuyến tính, chưa tổ chức theo lập luận.",
                    "Dùng lặp một vài từ nối quen thuộc cho hầu hết các quan hệ.",
                    "Tham chiếu bằng đại từ đôi chỗ không rõ trỏ về đâu.",
                    "Phần triển khai mỏng so với phần mở đầu, ý chính không được đỡ bằng dẫn chứng.")),
            FRAMEWORK_VERSION_BAND_CODE_4, new BandContent(
                "Bố cục rõ ràng và có chủ đích: ý chính được nêu, triển khai bằng lý do và ví dụ, rồi chốt lại. "
                    + "Các ý được liên kết bằng phương tiện đa dạng -- từ nối, tham chiếu, lặp từ khoá có kiểm "
                    + "soát -- chứ không chỉ bằng liên từ. Người nghe theo được cả mạch lập luận chứ không chỉ "
                    + "mạch thông tin. Hạn chế còn lại nằm ở sự cân đối giữa các phần và ở chỗ khung tổ chức đôi "
                    + "khi lộ ra.",
                List.of(
                    "Dùng từ nối phong phú để đánh dấu quan hệ giữa các ý (tương phản, nhượng bộ, hệ quả).",
                    "Người nghe theo được mạch lập luận chứ không chỉ mạch thông tin.",
                    "Liên kết bằng cả tham chiếu và lặp từ khoá có kiểm soát, không chỉ bằng liên từ.",
                    "Triển khai ý chính bằng lý do và ví dụ rồi chốt lại rõ ràng."),
                List.of(
                    "Một vài đoạn triển khai chưa cân đối về độ dài so với tầm quan trọng của ý.",
                    "Đôi chỗ dùng từ nối hơi dày, nghe như đang đọc theo khung có sẵn.",
                    "Phần kết đôi khi chỉ nhắc lại ý đã nói mà không nâng lên.",
                    "Khi bị hỏi ngược, mạch tổ chức lỏng hơn so với phần đã chuẩn bị.")),
            FRAMEWORK_VERSION_BAND_CODE_5, new BandContent(
                "Lập luận chặt chẽ và mạch ý được duy trì xuyên suốt cả đoạn dài, kể cả khi có nhiều tuyến ý "
                    + "song song. Dùng cấu trúc diễn ngôn -- nêu luận điểm, dự đoán phản bác, quay lại chốt -- để "
                    + "dẫn người nghe đi theo. Các phương tiện liên kết đa dạng và kín đáo, không lộ ra như khung "
                    + "mẫu. Rất hiếm chỗ khiến người nghe mất mạch.",
                List.of(
                    "Dùng cấu trúc diễn ngôn để dẫn người nghe theo lập luận (nêu luận điểm, dự đoán phản bác, chốt lại).",
                    "Duy trì được nhiều tuyến ý song song mà không để người nghe lẫn.",
                    "Phương tiện liên kết đa dạng và kín đáo, không lộ ra như khung mẫu.",
                    "Cân đối được độ dài các phần theo tầm quan trọng của ý."),
                List.of(
                    "Vẫn còn một hai chỗ khiến người nghe phải bám lại mạch, dù rất hiếm.",
                    "Ở chủ đề trừu tượng, đôi khi phải quay lại làm rõ ý vừa nói.",
                    "Một vài chuyển đoạn được đánh dấu rõ hơn mức cần thiết.")),
            FRAMEWORK_VERSION_BAND_CODE_6, new BandContent(
                "Tổ chức ý mạch lạc và tự nhiên như một bài nói đã chuẩn bị kỹ, dù đang nói ứng khẩu. Điều "
                    + "hướng mạch ý hoàn toàn linh hoạt -- mở ngoặc, rẽ nhánh, quay lại đúng chỗ -- mà vẫn giữ "
                    + "được trọng tâm. Liên kết được thực hiện chủ yếu bằng logic nội tại và tham chiếu chứ không "
                    + "cần từ nối lộ liễu. Không có chỗ nào làm gián đoạn mạch hiểu của người nghe.",
                List.of(
                    "Điều hướng mạch ý linh hoạt -- mở ngoặc, rẽ nhánh, quay lại đúng chỗ -- mà vẫn giữ được trọng tâm.",
                    "Liên kết chủ yếu bằng logic nội tại và tham chiếu, không cần từ nối lộ liễu.",
                    "Bố cục chặt như bài đã chuẩn bị dù đang nói ứng khẩu.",
                    "Chủ động dẫn sự chú ý của người nghe đến đúng điểm mấu chốt."),
                List.of(
                    "Nếu có bất kỳ chỗ nào làm gián đoạn mạch hiểu của người nghe, chưa đạt bậc này.",
                    "Nếu mạch lạc đạt được nhờ bám khung mẫu có sẵn thay vì logic nội tại, chưa đạt bậc này.",
                    "Nếu tổ chức ý lỏng đi khi bị hỏi ngược hoặc phải ứng biến, chỉ nên xếp bậc dưới.",
                    "Nếu phải quay lại làm rõ ý đã nói, chỉ nên xếp bậc dưới."))
        );
    }

    private record CriterionSeed(String code, String name, String description, int order) {}

    private record BandSeed(String code, String label, String description, int order) {}

    // Band content gồm có descriptor, positive signals và negative signals
    // 3 phần này càng miêu tả chuẩn, chi tiết, model sẽ càng đưa ra kết quả chuẩn nhất có thể
    // Cho nên 3 phần này phải lưu càng nhiều thông tin càng tốt, có thể lên đến hơn trăm từ cho một đoạn descriptor hoặc signal nếu cần
    //
    // Ba quy ước khi viết thêm nội dung ở đây:
    //
    // 1. Descriptor phải TỰ ĐỦ. Đường luyện tập (PracticeEvaluationRequestFactory) gửi sang
    //    model danh sách signal RỖNG -- chỉ descriptor đi tới nơi. Cho nên mọi thứ then chốt
    //    để xếp đúng bậc phải nằm trong descriptor; signal là phần bổ sung cho đường đề thi.
    //
    // 2. Signal viết theo thứ tự GIẢM DẦN độ quyết định, vì HIGH_IMPORTANCE_SIGNAL_COUNT phần
    //    tử đầu được đánh importance HIGH.
    //
    // 3. Ở bậc cao nhất, "dấu hiệu tiêu cực" không còn là lỗi còn sót mà là ĐIỀU KIỆN LOẠI:
    //    viết dạng "nếu còn X thì chưa đạt bậc này". Bậc 6 mà mô tả tiêu cực kiểu "không còn
    //    lỗi nào" thì thực chất là một dấu hiệu tích cực nằm nhầm chỗ, không giúp model phân
    //    biệt được bậc 6 với bậc 5.
    private record BandContent(String descriptor, List<String> positive, List<String> negative) {}
}
