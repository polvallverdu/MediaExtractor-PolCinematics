package dev.polv.mediaextractor;

import java.awt.image.BufferedImage;

public class VideoExtractor extends Extractor {
    private final int width;
    private final int height;

    public VideoExtractor(String path) {
        super(path, false);

        this.startCaching();

        this.width = this.frameGrabber.getImageWidth();
        this.height = this.frameGrabber.getImageHeight();
    }

    public BufferedImage getBufferedImage(long framen) {
        return (BufferedImage) this.getFrame(framen);
    }

    public BufferedImage getBufferedImageByMili(long mili) {
        return this.getBufferedImage(miliToFrame(mili));
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
