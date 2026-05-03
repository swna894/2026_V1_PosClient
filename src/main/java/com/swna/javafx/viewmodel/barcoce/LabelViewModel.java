package com.swna.javafx.viewmodel.barcoce;


import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import com.swna.javafx.service.barcode.LabelPrintService;

import reactor.core.Disposable;

/**
 * Label ViewModel
 *
 * 역할:
 *  - UI 상태 관리
 *  - 버튼 이벤트 처리
 *  - loading 상태 관리
 *  - status 메시지 관리
 *  - 비동기 작업 제어
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Getter
public class LabelViewModel {

    private final LabelPrintService labelPrintService;

    /**
     * 로딩 상태
     */
    private final BooleanProperty loading =
            new SimpleBooleanProperty(false);

    /**
     * 상태 메시지
     */
    private final StringProperty status =
            new SimpleStringProperty("Ready");

    /**
     * 진행중 작업
     */
    private Disposable currentTask;

    /**
     * 라벨 생성 실행
     */
    public void generateLabels() {

        // 중복 실행 방지
        if (loading.get()) {

            log.warn(
                    "LABEL GENERATE ALREADY RUNNING"
            );

            return;
        }

        log.info(
                "VIEWMODEL GENERATE LABELS"
        );

        loading.set(true);

        status.set(
                "Generating labels PDF..."
        );

        currentTask = labelPrintService.generateLabels()

                .doFinally(signal -> {

                    Platform.runLater(() -> {

                        loading.set(false);

                        log.info(
                                "LABEL GENERATE FINALLY"
                        );
                    });
                })

                .subscribe(

                        // success
                        null,

                        // error
                        error -> {

                            Platform.runLater(() -> {

                                status.set(
                                        "Generate failed"
                                );

                                log.error(
                                        "VIEWMODEL ERROR",
                                        error
                                );
                            });
                        },

                        // complete
                        () -> {

                            Platform.runLater(() -> {

                                status.set(
                                        "PDF generated"
                                );

                                log.info(
                                        "VIEWMODEL COMPLETE"
                                );
                            });
                        }
                );
    }

    /**
     * 작업 취소
     */
    public void cancel() {

        if (currentTask != null
                && !currentTask.isDisposed()) {

            currentTask.dispose();

            loading.set(false);

            status.set("Cancelled");

            log.warn(
                    "LABEL GENERATE CANCELLED"
            );
        }
    }

    /**
     * 상태 초기화
     */
    public void reset() {

        loading.set(false);

        status.set("Ready");

        log.info(
                "VIEWMODEL RESET"
        );
    }
}