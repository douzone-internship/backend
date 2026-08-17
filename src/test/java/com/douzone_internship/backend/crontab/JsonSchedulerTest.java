package com.douzone_internship.backend.crontab;

import com.douzone_internship.backend.service.DataInsertService;
import com.douzone_internship.backend.service.JsonBatchService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 배치 실패가 성공으로 집계되지 않는지 확인하는 회귀 테스트.
 *
 * batchAndSave()가 예외를 삼키면 성공 카운터가 증가하고
 * BatchFreshnessHealthIndicator까지 UP으로 보고하게 된다.
 * 그 경우 관측 지표를 갖춰두고도 배치 실패를 감지할 수 없다.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JsonScheduler 단위 테스트")
class JsonSchedulerTest {

    @Mock
    private JsonBatchService jsonBatchService;
    @Mock
    private DataInsertService dataInsertService;

    private MeterRegistry meterRegistry;
    private JsonScheduler jsonScheduler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        jsonScheduler = new JsonScheduler(jsonBatchService, dataInsertService, meterRegistry);
    }

    private double counter(String result) {
        var c = meterRegistry.find("batch.hira.runs")
                .tag("result", result)
                .tag("trigger", "monthly")
                .counter();
        return c == null ? 0d : c.count();
    }

    @Test
    @DisplayName("다운로드 실패 시 실패로 기록한다 - 성공 카운터는 증가하지 않는다")
    void recordsFailureWhenDownloadFails() throws IOException {
        willThrow(new IOException("clinic.json 다운로드가 4회 모두 실패"))
                .given(jsonBatchService).batchAndSave();

        jsonScheduler.runMonthly();

        assertThat(counter("failure")).isEqualTo(1d);
        assertThat(counter("success")).isZero();
        assertThat(jsonScheduler.getLastSuccessTime()).isNull();
        assertThat(jsonScheduler.getLastFailureTime()).isNotNull();
        assertThat(jsonScheduler.getLastFailureMessage()).contains("4회 모두 실패");
        // 다운로드가 실패했으면 DB 적재는 시도조차 하면 안 된다
        verify(dataInsertService, never()).insertOpenDataToDB();
    }

    @Test
    @DisplayName("DB 적재 단계에서 실패해도 실패로 기록한다")
    void recordsFailureWhenInsertFails() {
        willThrow(new RuntimeException("DB 적재 실패"))
                .given(dataInsertService).insertOpenDataToDB();

        jsonScheduler.runMonthly();

        assertThat(counter("failure")).isEqualTo(1d);
        assertThat(counter("success")).isZero();
        assertThat(jsonScheduler.getLastSuccessTime()).isNull();
    }

    @Test
    @DisplayName("전 단계가 성공해야만 성공으로 기록한다")
    void recordsSuccessOnlyWhenEverythingSucceeds() {
        jsonScheduler.runMonthly();

        assertThat(counter("success")).isEqualTo(1d);
        assertThat(counter("failure")).isZero();
        assertThat(jsonScheduler.getLastSuccessTime()).isNotNull();
        assertThat(jsonScheduler.getLastFailureTime()).isNull();
    }
}
