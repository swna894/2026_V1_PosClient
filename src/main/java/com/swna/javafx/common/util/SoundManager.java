package com.swna.javafx.common.util;

import javafx.scene.media.AudioClip;

public class SoundManager {

    private SoundManager() { }

    private static final AudioClip SUCCESS =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/success.mp3")
                            .toExternalForm()
            );

    private static final AudioClip ERROR_SOUND =
            new AudioClip(
                    SoundManager.class
                            .getResource("/sounds/error.mp3")
                            .toExternalForm()
            );

    public static void success() {
        SUCCESS.play();
    }


    public static void playError() {
        ERROR_SOUND.play();
    }
}
