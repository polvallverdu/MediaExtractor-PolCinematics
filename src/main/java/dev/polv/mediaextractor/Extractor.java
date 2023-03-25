package dev.polv.mediaextractor;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public abstract class Extractor {

    private final int CACHE_SECONDS = 8;

    private final String path;
    protected FFmpegFrameGrabber frameGrabber = null;

    private AtomicLong frameCount = new AtomicLong(0);
    private final long maxFrameCount;

    private final double framerate;
    private final double realFramerate;

    private final ConcurrentHashMap<Long, Object> frameCache;
    private final ConcurrentHashMap<Long, Long> frameCacheTime;
    private List<Thread> cachingThreads;
    private final boolean audio;

    public static final int AUDIO_CHUNK_SIZE = 4096;

    public Extractor(String path, boolean audio) {
        this.path = path;
        this.frameCache = new ConcurrentHashMap<>();
        this.frameCacheTime = new ConcurrentHashMap<>();
        this.cachingThreads = Collections.synchronizedList(new ArrayList<>());

        this.frameGrabber = new FFmpegFrameGrabber(path);
        try {
            this.frameGrabber.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.realFramerate = this.frameGrabber.getFrameRate();
        this.framerate = audio ? this.frameGrabber.getSampleRate() / (double) AUDIO_CHUNK_SIZE : this.realFramerate; // For audio, framerate is the amount of samples per second divided by 4096
        this.maxFrameCount = this.frameGrabber.getLengthInFrames() == 0 ? Long.MAX_VALUE : this.frameGrabber.getLengthInFrames();

        this.audio = audio;
    }

    // Should be runned in a separate thread
    private void cacheFramesThread(int amount) {
        try {
            for (int i = 0; i < amount; i++) {
                long frameCount = this.frameCount.getAndIncrement();
                if (frameCount >= this.maxFrameCount) {
                    this.frameCount.set(this.maxFrameCount);
                    break;
                }

                if (this.frameCache.containsKey(frameCount))
                    continue;

                //System.out.println("Getting frame " + frameCount);
                Frame ff;
                while (true) {
                    if (this.audio) {
                        ff = this.frameGrabber.grabSamples().clone();
                    } else {
                        ff = this.frameGrabber.grabImage().clone();
                    }

                    if (ff == null || (audio && (ff.samples == null || ff.samples.length == 0)) || (!audio && ff.image == null)) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        continue;
                    }

                    break;
                }

                Object obj;
                if (this.audio) {
                    final ShortBuffer bufferSamples = (ShortBuffer) ff.samples[0];
                    bufferSamples.rewind();

                    final ByteBuffer outBuffer = ByteBuffer.allocate(bufferSamples.capacity() * 2);

                    for (int x = 0; x < bufferSamples.capacity(); x++) {
                        short val = bufferSamples.get(x);
                        outBuffer.putShort(val);
                    }
                    obj = outBuffer.array();
                } else {
                    Java2DFrameConverter c = new Java2DFrameConverter();
                    obj = c.convert(ff);
                }

                this.frameCache.put(frameCount, obj);
                this.frameCacheTime.put(frameCount, System.currentTimeMillis());
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
        t.start();
        /*this.cachingThreads.add(t);
        new Thread(() -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.cachingThreads.remove(t);
        }).start();*/

        new Thread(() -> {
            for (Long frame : new ArrayList<>(this.frameCacheTime.keySet())) {
                if (System.currentTimeMillis() - this.frameCacheTime.get(frame) > (CACHE_SECONDS*1000*5)) {
                    this.frameCache.remove(frame);
                    this.frameCacheTime.remove(frame);
                }
            }
        }).start();
        // System.out.println("Caching frames");
        // System.out.println("framerate: " + this.framerate);
        // System.out.println("maxframe: " + this.maxFrameCount);
        //this.cacheFramesThread(audio ? ((int) (CACHE_SECONDS*framerate)/4096) : ((int) framerate)*CACHE_SECONDS);
        // System.out.println("Done caching frames. Cache size: " + this.frameCache.size());
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
        this.frameCacheTime.clear();
    }

    public Object getFrame(long framen) {
        Object frame = this.frameCache.get(framen);
        this.frameCacheTime.put(framen, System.currentTimeMillis());
        if (frame == null) {
            this.frameCount.set(Math.min(Math.max(framen, 0), this.maxFrameCount));
            startCaching();
            this.cacheFramesThread(1);
            while (frame == null) {
                try {
                    Thread.sleep(1);
                    frame = this.frameCache.get(framen);
                    /*if (frame != null) {
                        if ((audio && (frame.samples == null || frame.samples.length == 0)) || (!audio && frame.image == null)) {
                            frame = null;
                        }
                    }*/
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        if (framen+((CACHE_SECONDS*1000)/2) >= this.frameCount.get())
            this.startCaching();

        return frame;
    }

    public Object getFrameByMili(long time) {
        return this.getFrame(miliToFrame(time));
    }

    protected long frameToMili(long frame) {
        return (long) (frame / this.realFramerate * 1000);
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
