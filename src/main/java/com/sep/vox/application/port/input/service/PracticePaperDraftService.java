package com.sep.vox.application.port.input.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sep.vox.application.exception.ForbiddenException;
import com.sep.vox.application.exception.NotFoundException;
import com.sep.vox.application.exception.QuotaExceededException;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaper;
import com.sep.vox.application.response.input.practiceplanning.PracticePlanningResponses.PracticePaperDraft;

/**
 * Điều phối dựng đề 2 pha.
 *
 * Vì sao cần: dựng đề rơi xuống bậc 4 (nhờ AI sinh câu mới) khi kho chưa có câu phù hợp,
 * mất 10-20s -- quá lâu để giữ một request HTTP (client timeout, thread Tomcat + connection
 * DB bị giữ theo). Nên tách: pha 1 khởi động rồi trả về ngay, pha 2 client hỏi lại.
 *
 * Vẫn CHỜ NGẮN ({@link #FAST_PATH_WAIT}) trước khi trả PREPARING: khi kho đã có câu (bậc 1,
 * vài chục ms -- đường đi thường gặp sau khi kho đầy) thì trả thẳng READY, học sinh không
 * phải chịu thêm một vòng hỏi lại chỉ để nhận thứ vốn đã sẵn sàng.
 *
 * Nhận {@code Supplier} thay vì tự gọi use case để không phụ thuộc ngược từ tầng service lên
 * tầng use case -- service này chỉ lo vòng đời draft + chạy nền, việc dựng đề do caller đưa vào.
 *
 * Lưu draft trong bộ nhớ, không xuống DB: mất khi restart app là chấp nhận được (học sinh
 * bấm lại là xong, bản thân practice_paper đã nằm trong DB với hạn giữ chỗ 10 phút), đổi lại
 * không phải thêm bảng + migration cho một thứ sống vài chục giây.
 */
@Service
public class PracticePaperDraftService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PracticePaperDraftService.class);
    private static final Duration FAST_PATH_WAIT = Duration.ofSeconds(2);
    private static final Duration DRAFT_TTL = Duration.ofMinutes(10);

    private record Entry(UUID studentId, PracticePaperDraft draft, Instant createdAt) {
    }

    private final Map<UUID, Entry> drafts = new ConcurrentHashMap<>();
    private final AsyncTaskExecutor executor;

    public PracticePaperDraftService(
            @Qualifier("practiceGenerationExecutor") AsyncTaskExecutor executor) {
        this.executor = executor;
    }

    public PracticePaperDraft start(UUID studentId, Supplier<PracticePaper> buildPaper) {
        evictExpired();
        var draftId = UUID.randomUUID();
        store(draftId, studentId, PracticePaperDraft.preparing(draftId));

        // Chạy trên executor riêng, KHÔNG trên thread request: đó là điều cho phép OSIV nhả
        // connection DB thay vì giữ suốt lúc chờ LLM (executor đã bọc SecurityContext nên
        // use case bên trong vẫn đọc được người dùng hiện tại).
        var future = CompletableFuture.supplyAsync(buildPaper, executor);

        try {
            var paper = future.get(FAST_PATH_WAIT.toMillis(), TimeUnit.MILLISECONDS);
            return store(draftId, studentId, PracticePaperDraft.ready(draftId, paper));
        } catch (TimeoutException timeout) {
            // Đang phải sinh câu mới -- để chạy tiếp nền, client hỏi lại sau.
            future.whenComplete((paper, error) -> store(
                draftId,
                studentId,
                error == null
                    ? PracticePaperDraft.ready(draftId, paper)
                    : failure(draftId, error)
            ));
            return PracticePaperDraft.preparing(draftId);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return store(
                draftId,
                studentId,
                PracticePaperDraft.failed(draftId, "Dựng đề bị gián đoạn, vui lòng thử lại.")
            );
        } catch (ExecutionException failed) {
            return store(draftId, studentId, failure(draftId, failed));
        }
    }

    public PracticePaperDraft get(UUID studentId, UUID draftId) {
        var entry = drafts.get(draftId);
        if (entry == null) {
            throw new NotFoundException("Không tìm thấy phiên dựng đề, vui lòng chọn lại chủ đề.");
        }
        if (!entry.studentId().equals(studentId)) {
            throw new ForbiddenException("Bạn không có quyền xem phiên dựng đề này.");
        }
        return entry.draft();
    }

    private PracticePaperDraft store(UUID draftId, UUID studentId, PracticePaperDraft draft) {
        drafts.put(draftId, new Entry(studentId, draft, Instant.now()));
        return draft;
    }

    private static PracticePaperDraft failure(UUID draftId, Throwable error) {
        LOGGER.warn("Dựng đề thất bại cho draft {}", draftId, error);
        return PracticePaperDraft.failed(draftId, readableReason(error));
    }

    /** Giữ nguyên thông báo của lỗi nghiệp vụ (hết hạn mức, chủ đề chưa có câu...) vì chúng
     * được viết để hiện thẳng cho học sinh; lỗi lạ thì trả câu chung, không lộ chi tiết kỹ thuật. */
    private static String readableReason(Throwable error) {
        var cause = error instanceof ExecutionException && error.getCause() != null
            ? error.getCause()
            : error;
        if (cause instanceof NotFoundException
                || cause instanceof QuotaExceededException
                || cause instanceof IllegalStateException
                || cause instanceof IllegalArgumentException) {
            return cause.getMessage();
        }
        return "Không dựng được đề luyện lúc này, vui lòng thử lại.";
    }

    private void evictExpired() {
        var cutoff = Instant.now().minus(DRAFT_TTL);
        drafts.entrySet().removeIf(entry -> entry.getValue().createdAt().isBefore(cutoff));
    }
}
