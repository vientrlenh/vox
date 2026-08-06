package com.sep.vox.application.port.output;

import java.util.List;

import com.sep.vox.application.response.output.PushDispatchResult;
import com.sep.vox.application.response.output.PushMessage;

public interface PushNotificationPort {

    /**
     * Gửi một thông báo tới nhiều thiết bị. Tự chia lô theo giới hạn của nhà cung cấp,
     * phía gọi truyền bao nhiêu FID cũng được.
     *
     * <p>Không ném exception khi một phần thiết bị lỗi -- lỗi từng thiết bị là chuyện
     * bình thường (gỡ app, đổi máy) và được trả về trong kết quả để phía gọi dọn dẹp.
     */
    PushDispatchResult send(PushMessage message, List<String> installationIds);

    /** Push có đang bật hay không. Cho phép phía gọi bỏ qua bước dựng nội dung khi tắt. */
    boolean isEnabled();
}
