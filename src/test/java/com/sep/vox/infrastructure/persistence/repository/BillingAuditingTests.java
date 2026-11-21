package com.sep.vox.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.sep.vox.domain.model.order.Order;
import com.sep.vox.domain.repository.OrderRepository;
import com.sep.vox.infrastructure.persistence.adapter.OrderRepositoryImpl;
import com.sep.vox.infrastructure.security.CustomUserDetails;
import com.sep.vox.support.RepositoryTest;

import jakarta.persistence.EntityManager;

@RepositoryTest
@Import(OrderRepositoryImpl.class)
class BillingAuditingTests {

    private static final String SYSTEM_ACTOR = "System";
    private static final int UUID_VERSION_7 = 7;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenNobodyIsLoggedIn_thenNewOrderHasVersionSevenIdEqualTimestampsAndSystemActor() {
        Order stored = reload(createTopUpOrder());

        assertThat(stored.getId().version()).isEqualTo(UUID_VERSION_7);
        assertThat(stored.getCreatedAt()).isNotNull().isEqualTo(stored.getUpdatedAt());
        assertThat(stored.getCreatedBy()).isEqualTo(SYSTEM_ACTOR);
        assertThat(stored.getUpdatedBy()).isEqualTo(SYSTEM_ACTOR);
    }

    @Test
    void whenAUserIsLoggedIn_thenBothActorsAreTheUserIdText() {
        UUID userId = UUID.randomUUID();
        logIn(userId);

        Order stored = reload(createTopUpOrder());

        assertThat(stored.getCreatedBy()).isEqualTo(userId.toString());
        assertThat(stored.getUpdatedBy()).isEqualTo(userId.toString());
        assertThat(stored.isCreatedBy(userId)).isTrue();
    }

    @Test
    void whenUpdated_thenCreationValuesStayAndUpdateValuesMove() {
        UUID creatorId = UUID.randomUUID();
        UUID editorId = UUID.randomUUID();
        logIn(creatorId);
        Order created = reload(createTopUpOrder());

        logIn(editorId);
        created.setNotes("reviewed");
        orderRepository.save(created);
        Order updated = reload(created);

        assertThat(updated.getNotes()).isEqualTo("reviewed");
        assertThat(updated.getCreatedAt()).isEqualTo(created.getCreatedAt());
        assertThat(updated.getCreatedBy()).isEqualTo(creatorId.toString());
        assertThat(updated.getUpdatedAt()).isAfter(created.getUpdatedAt());
        assertThat(updated.getUpdatedBy()).isEqualTo(editorId.toString());
    }

    private Order createTopUpOrder() {
        return orderRepository.save(Order.forTopUp(
            UUID.randomUUID(), "Top up", new BigDecimal("100000"), new BigDecimal("1000"), Instant.now()));
    }

    private Order reload(Order order) {
        entityManager.flush();
        entityManager.clear();
        return orderRepository.findById(order.getId()).orElseThrow();
    }

    private static void logIn(UUID userId) {
        CustomUserDetails principal = CustomUserDetails.builder().id(userId).build();
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
