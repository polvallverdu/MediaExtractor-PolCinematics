package dev.polv.mediaextractor;

import org.bytedeco.javacv.Frame;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

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

    public byte[] getAudioBytes(long framen) {
        Frame frame = this.getFrame(framen);
        ShortBuffer channelSamplesShortBuffer = (ShortBuffer) frame.samples[0];
        channelSamplesShortBuffer.rewind();

        final ByteBuffer outBuffer = ByteBuffer.allocate(channelSamplesShortBuffer.capacity() * 2);

        for (int x = 0; x < channelSamplesShortBuffer.capacity(); x++) {
            short val = channelSamplesShortBuffer.get(x);
            outBuffer.putShort(val);
        }
        return outBuffer.array();
    }

    public byte[] getAudioBytesByMili(long mili) {
        return this.getAudioBytes(miliToFrame(mili));
    }

    public byte[] getAudioBytes() {
        if (this.frameCount == getMaxFrameCount()) {
            return new byte[1024];
        }
        long oldFrameCount = this.frameCount;
        this.frameCount++;
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

}
