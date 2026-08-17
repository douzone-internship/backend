package com.douzone_internship.backend.crontab;

import com.douzone_internship.backend.service.DataInsertService;
import com.douzone_internship.backend.service.JsonBatchService;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.stream.Stream;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@Getter
public class JsonScheduler {

    private final JsonBatchService jsonBatchService;
    private final DataInsertService dataInsertService;
    private final MeterRegistry meterRegistry;

    private final Path dataDir = Paths.get("data");

    // 기동 시 자동 배치는 실제 data.go.kr을 11회 호출한다. 테스트(@SpringBootTest)에서까지
    // 돌면 외부 API에 의존하는 느리고 불안정한 스위트가 되므로 끌 수 있게 한다.
    @Value("${hira.batch.run-on-startup:true}")
    private boolean runOnStartup = true;

    private volatile Instant lastSuccessTime;
    private volatile Instant lastFailureTime;
    private volatile String lastFailureMessage;

    @EventListener(ApplicationReadyEvent.class)
    public void checkAndRunOnStartup() {
        if (!runOnStartup) {
            log.info("hira.batch.run-on-startup=false 이므로 기동 시 배치를 건너뜀");
            return;
        }
        try {
            if (!Files.exists(dataDir)) {
                log.info("/data 폴더가 없어 즉시 Json 다운로드 실행 및 DB에 저장");
                runBatch("startup");
                return;
            }

            try (Stream<Path> files = Files.list(dataDir)) {
                boolean isEmpty = files.findAny().isEmpty();
                if (isEmpty) {
                    log.info("/data 폴더가 비어있어 즉시 Json 다운로드 실행 및 DB에 저장");
                    runBatch("startup");
                } else {
                    log.info("/data 폴더에 파일이 존재하여 실행하지 않음");
                }
            }
        } catch (IOException e) {
            log.error("/data 폴더 확인 중 오류 발생", e);
        }
    }

    @Scheduled(cron = "0 0 0 1 * *")
    public void runMonthly() {
        log.info("월간 스케줄 실행 - Json 다운로드");
        runBatch("monthly");
    }

    private void runBatch(String trigger) {
        try {
            jsonBatchService.batchAndSave();
            dataInsertService.insertOpenDataToDB();
            lastSuccessTime = Instant.now();
            meterRegistry.counter("batch.hira.runs", "result", "success", "trigger", trigger).increment();
            log.info("배치 성공: trigger={}", trigger);
        } catch (Exception e) {
            lastFailureTime = Instant.now();
            lastFailureMessage = e.getMessage();
            meterRegistry.counter("batch.hira.runs", "result", "failure", "trigger", trigger).increment();
            log.error("배치 실패: trigger={}", trigger, e);
        }
    }
}