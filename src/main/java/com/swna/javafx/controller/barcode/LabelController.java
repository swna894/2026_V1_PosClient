package com.swna.javafx.controller.barcode;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

import org.springframework.stereotype.Component;

import com.swna.javafx.viewmodel.barcoce.LabelViewModel;

/**
 * Label Controller
 *
 * 역할:
 *  - FXML UI 제어
 *  - View ↔ ViewModel 연결
 *  - 사용자 이벤트 처리
 */
@Component
@RequiredArgsConstructor
@Slf4j
@FxmlView("/view/barcode/LabelView.fxml")
public class LabelController {

    private final LabelViewModel viewModel;

    /**
     * PDF 생성 버튼
     */
    @FXML
    private Button btnGenerate;

    /**
     * 취소 버튼
     */
    @FXML
    private Button btnCancel;

    /**
     * 로딩 표시
     */
    @FXML
    private ProgressIndicator progressIndicator;

    /**
     * 상태 메시지
     */
    @FXML
    private Label lblStatus;

    /**
     * 초기화
     */
    @FXML
    public void initialize() {

        log.info(
                "LABEL CONTROLLER INITIALIZE"
        );

        // loading 상태 바인딩
        progressIndicator.visibleProperty()
                .bind(
                        viewModel.getLoading()
                );

        // 생성 버튼 비활성화
        btnGenerate.disableProperty()
                .bind(
                        viewModel.getLoading()
                );

        // 취소 버튼 활성화
        btnCancel.disableProperty()
                .bind(
                        viewModel.getLoading()
                                .not()
                );

        // 상태 메시지 바인딩
        lblStatus.textProperty()
                .bind(
                        viewModel.getStatus()
                );
    }

    /**
     * PDF 생성 버튼 클릭
     */
    @FXML
    public void onGenerate() {

        log.info(
                "BUTTON GENERATE CLICK"
        );

        viewModel.generateLabels();
    }

    /**
     * 취소 버튼 클릭
     */
    @FXML
    public void onCancel() {

        log.warn(
                "BUTTON CANCEL CLICK"
        );

        viewModel.cancel();
    }

    /**
     * 화면 종료 시 처리
     */
    public void dispose() {

        log.info(
                "LABEL CONTROLLER DISPOSE"
        );

        viewModel.cancel();
    }
}