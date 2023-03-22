package dev.polv.mediaextractor;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Extractor {

    private final int CACHE_SECONDS = 10;

    private final String path;
    protected FFmpegFrameGrabber frameGrabber = null;

    private AtomicLong frameCount = new AtomicLong(0);
    private final long maxFrameCount;

    private final double framerate;

    private final ConcurrentHashMap<Long, Frame> frameCache;
    private List<Thread> cachingThreads;
    private final boolean audio;

    public Extractor(String path, boolean audio) {
        this.path = path;
        this.frameCache = new ConcurrentHashMap<>();
        this.cachingThreads = Collections.synchronizedList(new ArrayList<>());

        this.frameGrabber = new FFmpegFrameGrabber(path);
        this.framerate = this.frameGrabber.getFrameRate();
        this.maxFrameCount = this.frameGrabber.getLengthInFrames();

        this.audio = audio;
    }

    // Should be runned in a separate thread
    private void cacheFramesThread(int amount) {
        try {
            if (this.cachingThreads.size() <= 1) {
                this.frameGrabber.start();
            }
            for (int i = 0; i < amount; i++) {
                long frameCount = this.frameCount.getAndIncrement();
                if (frameCount >= this.maxFrameCount) {
                    this.frameCount.set(this.maxFrameCount);
                    break;
                }

                if (this.frameCache.containsKey(frameCount))
                    continue;

                Frame frame;
                if (this.audio) {
                    frame = this.frameGrabber.grabFrame(true, false, true, false);
                } else {
                    frame = this.frameGrabber.grabImage();
                }
                this.frameCache.put(frameCount, frame);
            }
            if (this.cachingThreads.size() <= 1) {
                this.frameGrabber.stop();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void changeCurrentFrame(long newFrame) {
        this.frameCount.set(Math.min(Math.max(newFrame, 0), this.maxFrameCount));
        this.cacheCheck();
    }

    /**
     * @param newTime in milliseconds
     */
    public void changeCurrentTime(long newTime) {
        this.changeCurrentFrame(miliToFrame(newTime));
    }

    public void startCaching() {
        Thread t = new Thread(() -> this.cacheFramesThread(((int) framerate)*CACHE_SECONDS));
        this.cachingThreads.add(t);
        t.start();
        new Thread(() -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.cachingThreads.remove(t);
        }).start();
    }

    public void cacheCheck() {
        long frameCount = this.frameCount.get();
        if (this.frameCache.containsKey(frameCount+(((long) framerate)*CACHE_SECONDS)))
            return;

        this.startCaching();
    }

    public void stop() {
        this.cachingThreads.forEach(Thread::interrupt);
        this.cachingThreads.clear();
        try {
            this.frameGrabber.stop();
        } catch (FFmpegFrameGrabber.Exception e) {
            e.printStackTrace();
        }
    }

    public void clearCache() {
        this.frameCache.clear();
    }

    public Frame getFrame(long framen) {
        Frame frame = this.frameCache.get(framen);
        if (frame == null) {
            this.changeCurrentFrame(framen);
            this.cacheFramesThread(1);
            frame = this.frameCache.get(framen);
        }

        return frame;
    }

    public Frame getFrameByMili(long time) {
        return this.getFrame(miliToFrame(time));
    }

    protected long frameToMili(long frame) {
        return (long) (frame / (this.framerate / 1000));
    }

    protected long miliToFrame(long mili) {
        return (long) (mili * (this.framerate / 1000));
    }

    public Duration getDuration() {
        return Duration.ofMillis(frameToMili(this.maxFrameCount));
    }

    public String getPath() {
        return path;
    }

    public AtomicLong getFrameCount() {
        return frameCount;
    }

    public long getMaxFrameCount() {
        return maxFrameCount;
    }

    public double getFramerate() {
        return framerate;
    }

    public boolean isAudio() {
        return audio;
    }
}
