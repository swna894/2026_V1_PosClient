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

    public void handleCartAction(PosViewModel viewModel, Button source, Runnable onUpdate) {
        if (viewModel.hasItems()) {
            // 현재 상품 있음 → HOLD 저장
            viewModel.holdCart();
            onUpdate.run();
            source.getStyleClass().add("cart-held");
        } else if (viewModel.hasHoldItems()) {
            // 비어있고 HOLD 존재 → 복원
            viewModel.resumeCart();
            onUpdate.run();
            source.getStyleClass().remove("cart-held");
        } else {
            viewModel.scanStatusProperty().set("No hold cart");
        }
    }
}
