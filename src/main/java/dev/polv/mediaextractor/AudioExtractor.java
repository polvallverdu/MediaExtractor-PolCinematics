package dev.polv.mediaextractor;

import org.bytedeco.javacv.Frame;

import java.nio.ByteBuffer;
import java.nio.ShortBuffer;

public class AudioExtractor extends Extractor {

    private final int samplerate;
    private final int audioChannels;


    public AudioExtractor(String path) {
        super(path, true);

        this.samplerate = this.frameGrabber.getSampleRate();
        this.audioChannels = this.frameGrabber.getAudioChannels();
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

    public int getSamplerate() {
        return samplerate;
    }

    public int getAudioChannels() {
        return audioChannels;
    }
}
