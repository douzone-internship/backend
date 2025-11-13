package com.douzone_internship.backend.crontab;

import com.douzone_internship.backend.service.DataInsertService;
import com.douzone_internship.backend.service.JsonBatchService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class jsonScheduler {

    private final JsonBatchService jsonBatchService;

    private final DataInsertService dataInsertService;

    private final Path dataDir = Paths.get("data");

    // 애플리케이션 시작 시 /data 폴더 확인 후 비어있으면 실행
    @EventListener(ApplicationReadyEvent.class)
    public void checkAndRunOnStartup() {
        try {
            if (!Files.exists(dataDir)) {
                log.info("/data 폴더가 없어 즉시 Json 다운로드 실행 및 DB에 저장");
                jsonBatchService.batchAndSave();
                dataInsertService.insertOpenDataToDB();
                return;
            }

            try (Stream<Path> files = Files.list(dataDir)) {
                boolean isEmpty = files.findAny().isEmpty();
                if (isEmpty) {
                    log.info("/data 폴더가 비어있어 즉시 Json 다운로드 실행 및 DB에 저장");
                    jsonBatchService.batchAndSave();
                    dataInsertService.insertOpenDataToDB();

                } else {
                    log.info("/data 폴더에 파일이 존재하여 실행하지 않음");
                }
            }
        } catch (IOException e) {
            log.error("/data 폴더 확인 중 오류 발생", e);
        }
    }

    // 매월 1일 자정(00:00:00)에 실행
    @Scheduled(cron = "0 0 0 1 * *")
    public void runMonthly() {
        log.info("월간 스케줄 실행 - Json 다운로드");
        jsonBatchService.batchAndSave();
    }
}
