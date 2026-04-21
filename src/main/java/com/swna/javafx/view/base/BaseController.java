package com.swna.javafx.view.base;

import com.swna.javafx.navigation.SceneManager;
import com.swna.javafx.view.base.support.AlertSupport;
import com.swna.javafx.view.base.support.AsyncSupport;
import com.swna.javafx.view.base.support.ExceptionSupport;
import com.swna.javafx.view.base.support.NavigationSupport;

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
