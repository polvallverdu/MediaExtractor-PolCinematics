package dev.polv.mediaextractor;

import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.image.BufferedImage;

public class VideoExtractor extends Extractor {
    private final int width;
    private final int height;

    public VideoExtractor(String path) {
        super(path, false);

        this.width = this.frameGrabber.getImageWidth();
        this.height = this.frameGrabber.getImageHeight();
    }

    public BufferedImage getFrameImage(long framen) {
        Frame frame = this.getFrame(framen);
        Java2DFrameConverter converter = new Java2DFrameConverter();
        return converter.convert(frame);
    }
}
