package com.sep.vox.domain.repository;

import java.util.Optional;

import com.sep.vox.domain.model.financial.CurrencyCode;
import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;

public interface ExchangeRateSnapshotRepository {
    ExchangeRateSnapshot save(ExchangeRateSnapshot snapshot);

    /**
     * Lần fetch gần nhất đã thành công CHO ĐÚNG ĐỒNG TIỀN NÀY -- rỗng nếu chưa có lần nào.
     *
     * <p>Bắt buộc lọc theo currencyCode chứ không lấy bừa bản ghi mới nhất: bảng lưu tỷ giá quy về VND
     * của NHIỀU đồng tiền (cột currency_code), nên "mới nhất" không kèm điều kiện sẽ trả về đồng tiền
     * nào vừa được job chạy sau cùng. Hôm nay chỉ có USD nên chưa sai, nhưng thêm đồng thứ hai là giá
     * bán quota lặng lẽ nhảy sang tỷ giá của đồng đó -- không có lỗi nào báo ra.
     */
    Optional<ExchangeRateSnapshot> findLatest(CurrencyCode currencyCode);
}
