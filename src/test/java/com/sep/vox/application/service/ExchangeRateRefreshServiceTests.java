package com.sep.vox.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sep.vox.domain.model.financial.ExchangeRateSnapshot;
import com.sep.vox.domain.repository.ExchangeRateSnapshotRepository;
import com.sep.vox.infrastructure.client.ExchangeRateApiClient;
import com.sep.vox.infrastructure.properties.ExchangeRateApiProperties;
import com.sep.vox.infrastructure.service.ExchangeRateRefreshService;

class ExchangeRateRefreshServiceTests {

    @Test
    void refreshSavesSnapshotWhenClientReturnsRate() {
        var client = mock(ExchangeRateApiClient.class);
        var repository = mock(ExchangeRateSnapshotRepository.class);
        var properties = new ExchangeRateApiProperties(null, null, null);
        when(client.fetchUsdToVndRate()).thenReturn(Optional.of(new BigDecimal("26777")));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

        var service = new ExchangeRateRefreshService(client, repository, properties);
        service.refresh();

        var captor = ArgumentCaptor.forClass(ExchangeRateSnapshot.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getUsdToVndRate()).isEqualByComparingTo(new BigDecimal("26777"));
        assertThat(captor.getValue().getSource()).isEqualTo(properties.baseUrl());
    }

    @Test
    void refreshSkipsSaveWhenClientReturnsEmpty() {
        var client = mock(ExchangeRateApiClient.class);
        var repository = mock(ExchangeRateSnapshotRepository.class);
        var properties = new ExchangeRateApiProperties(null, null, null);
        when(client.fetchUsdToVndRate()).thenReturn(Optional.empty());

        var service = new ExchangeRateRefreshService(client, repository, properties);
        service.refresh();

        verify(repository, never()).save(any());
    }
}
