package dev.polv.mediaextractor;

import org.bytedeco.javacv.Frame;

import javax.sound.sampled.AudioFormat;
import java.nio.ByteBuffer;
import java.nio.ShortBuffer;
import java.time.Duration;

public class AudioExtractor extends Extractor {

    private final int samplerate;
    private final int audioChannels;

    private long frameCount;


    public AudioExtractor(String path) {
        super(path, true);

        this.samplerate = this.frameGrabber.getSampleRate();
        this.audioChannels = this.frameGrabber.getAudioChannels();

        this.frameCount = 0;
    }

    public AudioFormat getAudioFormat() {
        return new AudioFormat(this.samplerate, 16, this.audioChannels, true, true);
    }

    public byte[] getAudioBytes(long framen) {
        byte[] frame = (byte[]) this.getFrame(framen);
        /*if (frame.samples == null || frame.samples.length == 0 || frame.samples[0] == null) {
            return new byte[4096];
        }

        final ShortBuffer bufferSamples = (ShortBuffer) frame.samples[0];
        bufferSamples.rewind();

        final ByteBuffer outBuffer = ByteBuffer.allocate(bufferSamples.capacity() * 2);

        for (int x = 0; x < bufferSamples.capacity(); x++) {
            short val = bufferSamples.get(x);
            outBuffer.putShort(val);
        }
        return outBuffer.array();*/
        return frame;
    }

    public byte[] getAudioBytesByMili(long mili) {
        return this.getAudioBytes(miliToFrame(mili));
    }

    public byte[] getAudioBytes() {
        if (this.frameCount == getMaxFrameCount()) {
            return new byte[4096];
        }
        long oldFrameCount = this.frameCount;
        this.frameCount++;
        // System.out.println("Fetching frame " + this.frameCount);
        return this.getAudioBytes(oldFrameCount);
    }

    @Override
    public void changeCurrentFrame(long newFrame) {
        this.frameCount = newFrame;
        super.changeCurrentFrame(newFrame);
    }

    public int getSamplerate() {
        return samplerate;
    }

    public int getAudioChannels() {
        return audioChannels;
    }

    // DEPRECATED FUNCTIONS BECAUSE DOESN'T WORK GREAT WITH AUDIO

    @Override
    @Deprecated
    public long getMaxFrameCount() {
        return super.getMaxFrameCount();
    }

    @Override
    @Deprecated
    public Duration getDuration() {
        return super.getDuration();
    }

}
