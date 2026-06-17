package com.swna.javafx.pos.manager;

import com.swna.javafx.pos.viewmodel.PosViewModel;
import javafx.scene.control.Button;
import org.springframework.stereotype.Component;

@Component
public class CartButtonManager {

    public void hideUnused(Button... buttons) {
        for (Button btn : buttons) {
            btn.setVisible(false);
            btn.setManaged(false);
        }
    }

    public void handleCartAction(PosViewModel viewModel, int targetCartId, Button source, Runnable onUpdate) {
        // 1. 현재 화면에 물건이 있다면 -> 현재 카트 번호(혹은 로직상 필요한 번호)로 저장
        if (!viewModel.getCartManager().isEmpty()) {
            viewModel.getHoldManager().save(targetCartId); 
        } 
        // 2. 해당 카트에 저장된 물건이 있다면 -> 불러오기
        else if (viewModel.getHoldManager().hasItems(targetCartId)) {
            viewModel.getHoldManager().resume(targetCartId);
        }
        
        onUpdate.run();
    }
}
