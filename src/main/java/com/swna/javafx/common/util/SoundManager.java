package com.swna.javafx.common.util;

import javafx.scene.media.AudioClip;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SoundManager {

    private SoundManager() { }

    private static final AudioClip SUCCESS;
    private static final AudioClip ERROR_SOUND;
    
    static {
        
        // 성공 사운드 로드
        var successUrl = SoundManager.class.getResource("/sounds/success2.mp3");
        
        if (successUrl != null) {
            SUCCESS = new AudioClip(successUrl.toExternalForm());
        } else {
            SUCCESS = null;
        }
        
        // 에러 사운드 로드
        var errorUrl = SoundManager.class.getResource("/sounds/error.mp3");
        
        if (errorUrl != null) {
            ERROR_SOUND = new AudioClip(errorUrl.toExternalForm());
        } else {
            ERROR_SOUND = null;
        }
        
    }

    public static void playSuccess() {
        if (SUCCESS != null) {
            log.debug("Playing success sound...");
            SUCCESS.setVolume(1.0);
            SUCCESS.play();
        } else {
            log.error("Cannot play success sound - SUCCESS is null (file not found or failed to load)");
        }
    }

    public static void playError() {
        if (ERROR_SOUND != null) {
            ERROR_SOUND.setVolume(1.0);
            ERROR_SOUND.play();
        } else {
            log.error("Cannot play error sound - ERROR_SOUND is null (file not found or failed to load)");
        }
    }
}