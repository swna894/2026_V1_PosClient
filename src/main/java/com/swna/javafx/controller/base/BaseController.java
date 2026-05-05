package com.swna.javafx.controller.base;

import com.swna.javafx.common.navigation.SceneManager;
import com.swna.javafx.controller.base.support.AlertSupport;
import com.swna.javafx.controller.base.support.AsyncSupport;
import com.swna.javafx.controller.base.support.ExceptionSupport;
import com.swna.javafx.controller.base.support.NavigationSupport;

public abstract class BaseController implements AlertSupport, NavigationSupport, AsyncSupport, ExceptionSupport {

    protected final SceneManager sceneManager;

    protected BaseController(SceneManager sceneManager) {
        this.sceneManager = sceneManager;
    }

    @Override
    public SceneManager getSceneManager() {
        return sceneManager;
    }

   @Override
   public AlertSupport getAlertSupport() {
      return this;
   }
}
