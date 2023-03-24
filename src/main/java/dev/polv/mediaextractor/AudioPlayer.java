package dev.polv.mediaextractor;

import javax.sound.sampled.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class AudioPlayer {

    enum PlayerStatus {
        STOPPED,
        PLAY,
        PAUSE,
        END,
    }

    private String path;
    private AudioExtractor extractor;

    private final AtomicReference<PlayerStatus> status = new AtomicReference<>(PlayerStatus.STOPPED);

    private final SourceDataLine soundLine;
    private final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();

    public AudioPlayer(String path) {
        this.path = path;
        this.extractor = new AudioExtractor(path);

        final AudioFormat audioFormat = new AudioFormat(this.extractor.getSamplerate(), 16, this.extractor.getAudioChannels(), true, true);
        final DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        try {
            soundLine = (SourceDataLine) AudioSystem.getLine(info);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    public void play() {
        if (status.get() != PlayerStatus.STOPPED) {
            return;
        }
        try {
            soundLine.open();
            status.set(PlayerStatus.PAUSE);
            this.resume();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        if (status.get() != PlayerStatus.PAUSE) {
            return;
        }
        status.set(PlayerStatus.PLAY);
        soundLine.start();
        audioExecutor.submit(() -> {
            while (status.get() == PlayerStatus.PLAY) {
                byte[] bb = extractor.getAudioBytes();
                soundLine.write(bb, 0, bb.length);
            }
        });
    }

    public void pause() {
        if (status.get() != PlayerStatus.PLAY) {
            return;
        }
        status.set(PlayerStatus.PAUSE);
    }

    public void stop() {
        soundLine.stop();
        soundLine.flush();
        soundLine.close();
        this.audioExecutor.shutdown();
        status.set(PlayerStatus.END);
    }

    public AudioExtractor getExtractor() {
        return extractor;
    }
}
