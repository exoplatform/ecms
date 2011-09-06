/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.impl;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 15, 2008 2:18:02 PM
 */
/**
 * Process images class
 */
public class ImageUtils {

  /**
   * Return the image which resized(support JPG, PNG, GIF)
   * @param image The BufferedImage which based on InputStream
   * BufferedImage image = ImageIO.read(InputStream)
   * @param maxWidth Max width of thumbnail will be resized
   * @param maxHeight Max height of thumbnail will be resized
   * @return InputStream
   * @throws Exception
   */
  public static InputStream scaleImage(BufferedImage image, int maxWidth, int maxHeight) throws Exception {
    return scaleImage(image, maxWidth, maxHeight, false);
  }

  /**
   * Return the image which resized(support JPG, PNG, GIF)
   * @param image The BufferedImage which based on InputStream
   * BufferedImage image = ImageIO.read(InputStream)
   * @param maxWidth Max width of thumbnail will be resized
   * @param maxHeight Max height of thumbnail will be resized
   * @return InputStream
   * @throws Exception
   */  
  public static InputStream scaleImage(BufferedImage image, int maxWidth, int maxHeight, boolean crop) throws Exception {
    // Make sure the aspect ratio is maintained, so the image is not skewed
    int imageWidth = image.getWidth(null);
    int imageHeight = image.getHeight(null);
    double widthRatio = (double) maxWidth / imageWidth;
    double heightRatio = (double) maxHeight / imageHeight;
    int dx, dy, dw, dh;
    dx = dy = 0;
    if (!crop) {
      if (maxWidth > 0 && imageWidth > maxWidth) {
        // Determine the shrink ratio
        imageHeight = (int) (imageHeight * widthRatio);
        imageWidth = maxWidth;
      }
      if (maxHeight > 0 && imageHeight > maxHeight) {
        // Determine the shrink ratio
        imageWidth = (int) (imageWidth * heightRatio);
        imageHeight = maxHeight;
      }
      dw = imageWidth;
      dh = imageHeight;
    } else {
      if (maxHeight==0) {
        imageHeight = (int) (imageHeight * widthRatio);
        imageWidth = maxWidth;
        dw = imageWidth;
        dh = imageHeight;
      } else if (maxWidth==0) {
        imageWidth = (int) (imageWidth * heightRatio);
        imageHeight = maxHeight;
        dw = imageWidth;
        dh = imageHeight;
      } else {
        double srcRatio = (double)imageWidth / imageHeight;
        double tgtRatio = (double)maxWidth / maxHeight;
        if (srcRatio<tgtRatio) {
          dx = 0;
          dw = maxWidth;
          dh = (int) (imageHeight * widthRatio);
          dy = (int) ((maxHeight/2) - (dh/2));
        } else {
          dy = 0;
          dh = maxHeight;
          dw = (int) (imageWidth * heightRatio);
          dx = (int) ((maxWidth/2) - (dw/2));
        }

        imageWidth = maxWidth;
        imageHeight = maxHeight;
      }
    }
    // Draw the scaled image
    BufferedImage thumbImage = new BufferedImage(imageWidth,
        imageHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics2D = thumbImage.createGraphics();
    graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics2D.drawImage(image, dx, dy, dw, dh, null);
    graphics2D.dispose();

    Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
    ImageWriter writer = iter.next();
    JPEGImageWriteParam iwp = (JPEGImageWriteParam)writer.getDefaultWriteParam();
    int quality = 85; // Use between 1 and 100, with 100 being highest quality
    quality = Math.max(0, Math.min(quality, 100));
    iwp.setCompressionMode(JPEGImageWriteParam.MODE_EXPLICIT);
    iwp.setCompressionQuality(quality / 100.0f);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    writer.setOutput(ImageIO.createImageOutputStream(out));
    IIOImage iioImage = new IIOImage(thumbImage, null, null);
    writer.write(null, iioImage, iwp);
    writer.dispose();    

    // Read the outputstream into the inputstream for the return value
    ByteArrayInputStream bis = new ByteArrayInputStream(out.toByteArray());
    return bis;
  }

}
