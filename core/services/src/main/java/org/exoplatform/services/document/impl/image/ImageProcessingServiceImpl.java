/**
 * Copyright (C) 2023 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
*/
package org.exoplatform.services.document.impl.image;

import org.exoplatform.services.document.image.ImageProcessingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class ImageProcessingServiceImpl implements ImageProcessingService
{

   private static final Log LOG = ExoLogger.getLogger("exo.core.component.document.ImageProcessingServiceImpl");

   public BufferedImage createCroppedImage(BufferedImage img, int chosenWidth, int chosenHeight)
   {
      int topX = 0; // X coordinate of the top left corner
      int topY = 0; // Y coordinate of the top left corner
      int newWidth = 0; // width of the scaled image before cropping
      int newHeight = 0; // height of the scaled image before cropping
      double factor = 0;
      double scaleHorizontalFactor = getScaleFactor(chosenWidth, img.getWidth());
      double scaleVerticalFactor = getScaleFactor(chosenHeight, img.getHeight());
      if (scaleVerticalFactor > scaleHorizontalFactor)
      {
         factor = scaleVerticalFactor;
         newWidth = (int)Math.round(img.getWidth() * factor);
         newHeight = (int)Math.round(img.getHeight() * factor);
         topX = (newWidth - chosenWidth) / 2;
      }
      else
      {
         factor = scaleHorizontalFactor;
         newWidth = (int)Math.round(img.getWidth() * factor);
         newHeight = (int)Math.round(img.getHeight() * factor);
         topY = (newHeight - chosenHeight) / 2;
      }

      // Scale the image: BufferedImage needed for "subImaging"
      BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
      Graphics2D aImage = scaledImage.createGraphics();
      aImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      aImage.drawImage(img, 0, 0, newWidth, newHeight, null);

      // Crop the image
      BufferedImage croppedImage = scaledImage.getSubimage(topX, topY, chosenWidth, chosenHeight);

      // Create the new image
      BufferedImage imageBuf = new BufferedImage(chosenWidth, chosenHeight, BufferedImage.TYPE_INT_RGB);
      Graphics2D newImage = imageBuf.createGraphics();
      newImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      newImage.drawImage(croppedImage, 0, 0, chosenWidth, chosenHeight, null);

      return imageBuf;
   }

   public BufferedImage createBoundImage(BufferedImage img, int chosenWidth, int chosenHeight, String bgColor)
   {
      // Calculate the scale factor according to bounds
      double factor = 0;
      double scaleHorizontalFactor = getScaleFactor(chosenWidth, img.getWidth());
      double scaleVerticalFactor = getScaleFactor(chosenHeight, img.getHeight());
      if (scaleVerticalFactor > scaleHorizontalFactor)
      {
         factor = scaleHorizontalFactor;
      }
      else
      {
         factor = scaleVerticalFactor;
      }

      int newWidth = (int)Math.round(img.getWidth() * factor);
      int newHeight = (int)Math.round(img.getHeight() * factor);

      // Scale the image
      Image imgScaled = img.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT);

      // Copy the scaled image into the new one
      BufferedImage imageBuf = new BufferedImage(chosenWidth, chosenHeight, BufferedImage.TYPE_INT_RGB);
      Graphics2D newImage = imageBuf.createGraphics();
      newImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      newImage.setBackground(hexToColor(bgColor)); // Set the background color
      newImage.clearRect(0, 0, chosenWidth, chosenHeight);
      newImage.drawImage(imgScaled, getImageOffset(chosenWidth, newWidth), getImageOffset(chosenHeight, newHeight),
         newWidth, newHeight, null);

      return imageBuf;
   }

   public BufferedImage createScaledImage(BufferedImage img, double factor)
   {
      // The original size
      int imgWidth = img.getWidth();
      int imgHeight = img.getHeight();
      // The scaled size
      int newWidth = (new Double(imgWidth * factor)).intValue();
      int newHeight = (new Double(imgHeight * factor)).intValue();

      // Scale the image
      Image imgScaled = img.getScaledInstance(newWidth, newHeight, Image.SCALE_DEFAULT);
      BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
      Graphics2D aImage = scaledImage.createGraphics();
      aImage.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      aImage.drawImage(imgScaled, 0, 0, newWidth, newHeight, null);

      return scaledImage;
   }

   private double getScaleFactor(int chosenDim, int imgDim)
   {
      return (double)chosenDim / (double)imgDim;
   }

   private int getImageOffset(int newDim, int oldDim)
   {
      int offset = 0;
      if (newDim >= oldDim)
      {
         offset = (newDim - oldDim) / 2;
      }
      return offset;
   }

   private Color hexToColor(final String rrggbb)
   {
      Color color = Color.WHITE; // Default value: white

      String hexColor = "";
      if (rrggbb.startsWith("#"))
      {
         hexColor = rrggbb.substring(1);
      }
      else
      {
         hexColor = rrggbb;
      }

      int length = hexColor.length();
      if (length == 3 || length == 6)
      {
         try
         {
            if (length == 3)
            { // Short syntax
               char c1, c2, c3;
               c1 = hexColor.charAt(0);
               c2 = hexColor.charAt(1);
               c3 = hexColor.charAt(2);
               color = Color.decode("0x" + c1 + c1 + c2 + c2 + c3 + c3);
            }
            else
            {
               color = Color.decode("0x" + hexColor);
            }
         }
         catch (NumberFormatException nfe)
         {
            if (LOG.isTraceEnabled())
            {
               LOG.trace("An exception occurred: " + nfe.getMessage());
            }
         }
      }

      return color;
   }

}
