package com.oplus.wrapper.media;

public final class AudioManager {
    private final android.media.AudioManager audioManager;

    public AudioManager(android.media.AudioManager audioManager) {
        this.audioManager = audioManager;
    }

    public int getRingerModeInternal() {
        return audioManager.getRingerMode();
    }
}
