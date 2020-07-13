package org.exoplatform.wcm.connector;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.jpeg.JpegDirectory;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageOrientation {
    public static final int ORIENTATION_Basique = 1;
    public static final int ORIENTATION_Flip_X = 2;
    public static final int ORIENTATION_PI = 3;
    public static final int ORIENTATION_Flip_Y= 4;
    public static final int ORIENTATION_PI_2F_lip_X= 5;
    public static final int ORIENTATION_PI_2_width =6;
    public static final int ORIENTATION_PI_2_Flip=7;
    public static final int ORIENTATION_PI_2=8;
    public final int orientation;
    public final int width;
    public final int height;
    private static final Log LOG = ExoLogger.getLogger(ImageOrientation.class.getName());

    public ImageOrientation(int orientation, int width, int height) {
        this.orientation = orientation;
        this.width = width;
        this.height = height;
    }

    public static ImageOrientation readImageInformation(InputStream imageFile) throws IOException, MetadataException, ImageProcessingException {
        Metadata metadata = ImageMetadataReader.readMetadata(imageFile);
        Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (directory == null) {
            LOG.warn("no EXIF info.");
        }
        JpegDirectory jpegDirectory = metadata.getFirstDirectoryOfType(JpegDirectory.class);
        int orientation;
        try {
            orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }  catch (MetadataException me) {
            orientation=1;
        }
        int width = jpegDirectory.getImageWidth();
        int height = jpegDirectory.getImageHeight();

        return new ImageOrientation(orientation, width, height);
    }

    public static AffineTransform getExifTransformation(ImageOrientation info) {

        AffineTransform affineTransform = new AffineTransform();

        switch (info.orientation) {
            case ORIENTATION_Basique:
                break;
            case ORIENTATION_Flip_X: // Flip X
                affineTransform.scale(-1.0, 1.0);
                affineTransform.translate(-info.width, 0);
                break;
            case ORIENTATION_PI: // PI rotation
                affineTransform.translate(info.width, info.height);
                affineTransform.rotate(Math.PI);
                break;
            case ORIENTATION_Flip_Y: // Flip Y
                affineTransform.scale(1.0, -1.0);
                affineTransform.translate(0, -info.height);
                break;
            case ORIENTATION_PI_2F_lip_X: // - PI/2 and Flip X
                affineTransform.rotate(-Math.PI / 2);
                affineTransform.scale(-1.0, 1.0);
                break;
            case ORIENTATION_PI_2_width: // -PI/2 and -width
                affineTransform.translate(info.height, 0);
                affineTransform.rotate(Math.PI / 2);
                break;
            case ORIENTATION_PI_2_Flip: // PI/2 and Flip
                affineTransform.scale(-1.0, 1.0);
                affineTransform.translate(-info.height, 0);
                affineTransform.translate(0, info.width);
                affineTransform.rotate(  3 * Math.PI / 2);
                break;
            case ORIENTATION_PI_2: // PI / 2
                affineTransform.translate(0, info.width);
                affineTransform.rotate(  3 * Math.PI / 2);
                break;
        }

        return affineTransform;
    }

    public static BufferedImage transformImage(BufferedImage image, AffineTransform transform) throws Exception {

        AffineTransformOp op = new AffineTransformOp(transform, AffineTransformOp.TYPE_BICUBIC);
        BufferedImage destinationImage = new BufferedImage(image.getWidth(),image.getHeight(), image.getType());
        destinationImage = op.filter(image, destinationImage);
        return destinationImage;
    }
}
