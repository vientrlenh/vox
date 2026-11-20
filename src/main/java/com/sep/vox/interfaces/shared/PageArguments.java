package com.sep.vox.interfaces.shared;

/**
 * Một chỗ duy nhất kiểm tham số phân trang, dùng chung cho mọi entry point (GraphQL lẫn REST).
 *
 * <p>Vì sao kiểm ở biên chứ không "chuẩn hoá" ở repository: bản trước mỗi query repository tự gọi
 * {@code Math.max(page, 0)} rồi {@code Math.min(size, MAX)}. Cách đó che mất đầu vào sai thay vì
 * báo lỗi -- một client gửi page 0 vẫn nhận về 200 OK kèm dữ liệu của trang khác, và không ai biết.
 * Tệ hơn, chính phép {@code Math.max(page, 0)} đó đã hợp thức hoá lối đếm từ 0 và giấu đi lỗi lệch
 * một trang suốt thời gian dài (xem {@code PagingConventionTests}).
 *
 * <p>Nay biên là chỗ DUY NHẤT quyết định đầu vào có hợp lệ hay không; repository tin vào đó và chỉ
 * còn đúng một việc: trừ 1 để đổi sang offset.
 */
public final class PageArguments {

    /**
     * Trần số dòng mỗi trang. Không phải con số nghiệp vụ mà là chặn trên an toàn: thiếu nó, một
     * yêu cầu {@code size=1000000} kéo cả bảng vào bộ nhớ. Đặt 200 vì client desktop cố ý xin một
     * trang 200 dòng cho màn chọn bài thi ({@code GET /api/v1/exams}).
     */
    public static final int MAX_PAGE_SIZE = 200;

    private PageArguments() {
    }

    /** Trang đếm từ 1. Ném {@link IllegalArgumentException} nếu thiếu hoặc ngoài khoảng cho phép. */
    public static void validate(Integer page, Integer size) {
        if (page == null || page <= 0) {
            throw new IllegalArgumentException("Số trang yêu cầu không hợp lệ");
        }
        if (size == null || size <= 0 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Kích cỡ trang yêu cầu không hợp lệ");
        }
    }
}
