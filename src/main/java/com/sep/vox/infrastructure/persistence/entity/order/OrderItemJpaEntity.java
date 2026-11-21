package com.sep.vox.infrastructure.persistence.entity.order;

import java.math.BigDecimal;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItemJpaEntity {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "order_id", nullable = false, updatable = false)
    private UUID orderId;

    @Column(name = "type", nullable = false, updatable = false, length = 20, check = {
        @CheckConstraint(
            name = "chk_order_items_type_valid",
            constraint = "type IN ('SUBSCRIPTION')"
        )
    })
    private String type;

    // Points to the entity matching `type` (currently only SUBSCRIPTION -> subscription_plans.id).
    // It CANNOT be an FK because it is polymorphic -- if only one kind remains later, turn it into a real FK.
    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "unit_price_vnd", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal unitPriceVnd;

    @Column(name = "amount_vnd", nullable = false, updatable = false, precision = 15, scale = 0)
    private BigDecimal amountVnd;

    @Column(name = "quantity", nullable = false, updatable = false)
    private Integer quantity;
}
