package com.swna.javafx.admin;

import org.springframework.stereotype.Component;

import javafx.fxml.FXML;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.rgielen.fxweaver.core.FxmlView;

@Slf4j
@Component
@RequiredArgsConstructor
@FxmlView("/view/admin/setting-view.fxml")
public class SettingViewController {

    // FXML에서 fx:id로 지정한 이름과 일치해야 합니다.
    /**
     * 화면이 로드될 때 자동으로 호출되는 초기화 메서드
     */
    @FXML
    public void initialize() {
  
    }

}