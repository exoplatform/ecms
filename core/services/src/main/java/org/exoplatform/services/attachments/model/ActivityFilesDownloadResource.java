/*
 * Copyright (C) 2003-2017 eXo Platform SAS.
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
package org.exoplatform.services.attachments.model;

import java.io.*;

import javax.jcr.Node;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.pdfbox.io.IOUtils;

import org.exoplatform.download.DownloadResource;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.services.wcm.core.NodetypeConstant;

/**
 * This class is used to generate a ZIP file containing multiple selected
 * JCR files.
 * The generated file is managed and exposed to end user via
 * the service org.exoplatform.web.handler.DownloadHandler
 */
public class ActivityFilesDownloadResource extends DownloadResource {
  private static final Log LOG = ExoLogger.getLogger(ActivityFilesDownloadResource.class);

  private NodeLocation[] nodeLocations;

  public ActivityFilesDownloadResource(NodeLocation[] nodelocations) {
    super(getMimetype(nodelocations));
    this.nodeLocations = nodelocations;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (nodeLocations == null || nodeLocations.length == 0) {
      return null;
    }

    if (nodeLocations.length == 1) {
      NodeLocation nodeLocation = nodeLocations[0];
      Node node = NodeLocation.getNodeByLocation(nodeLocation);
      try {
        if (node.isNodeType(NodetypeConstant.NT_FILE) && node.hasNode(NodetypeConstant.JCR_CONTENT)) {
          Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
          if (contentNode.hasProperty(NodetypeConstant.JCR_DATA)) {
            return contentNode.getProperty(NodetypeConstant.JCR_DATA).getStream();
          }
        }
      } catch (Exception e) {
        throw new IllegalStateException("Error getting inputstream of file content", e);
      }
      return null;
    } else {
      File resultFile = File.createTempFile("activity_files", ".zip");
      ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(resultFile);
      try {
        zipOutputStream.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.ALWAYS);
        zipOutputStream.setEncoding("UTF-8");
        for (NodeLocation nodeLocation : nodeLocations) {
          Node node = NodeLocation.getNodeByLocation(nodeLocation);
          if (node.isNodeType(NodetypeConstant.NT_FILE) && node.hasNode(NodetypeConstant.JCR_CONTENT)) {
            Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
            if (contentNode.hasProperty(NodetypeConstant.JCR_DATA)) {
              String fileName = node.hasProperty(NodetypeConstant.EXO_NAME) ? node.getProperty(NodetypeConstant.EXO_NAME).getString() : node.getName();
              ZipArchiveEntry entry = new ZipArchiveEntry(fileName);
              zipOutputStream.putArchiveEntry(entry);
              InputStream inputStream = null;
              try {
                inputStream = contentNode.getProperty(NodetypeConstant.JCR_DATA).getStream();
                IOUtils.copy(inputStream, zipOutputStream);
              } finally {
                if (inputStream != null) {
                  inputStream.close();
                }
              }
              zipOutputStream.closeArchiveEntry();
            }
          }
        }
      } catch (Exception exp) {
        IOUtils.closeQuietly(zipOutputStream);
        resultFile.delete();
        LOG.error("An error occurred when generating the ZIP file", exp);
        throw new RuntimeException("An error occurred when generating the ZIP file", exp);
      } finally {
        IOUtils.closeQuietly(zipOutputStream);
      }
      return new FileInputStream(resultFile);
    }
  }

  private static String getMimetype(NodeLocation[] nodeLocations) {
    if (nodeLocations == null || nodeLocations.length == 0) {
      return null;
    }

    if (nodeLocations.length == 1) {
      NodeLocation nodeLocation = nodeLocations[0];
      Node node = NodeLocation.getNodeByLocation(nodeLocation);
      try {
        if (node.isNodeType(NodetypeConstant.NT_FILE) && node.hasNode(NodetypeConstant.JCR_CONTENT)) {
          Node contentNode = node.getNode(NodetypeConstant.JCR_CONTENT);
          if (contentNode.hasProperty(NodetypeConstant.JCR_MIMETYPE)) {
            return contentNode.getProperty(NodetypeConstant.JCR_MIMETYPE).getString();
          }
        }
      } catch (Exception e) {
        throw new IllegalStateException("Error getting mimetype of file", e);
      }
      return "application/octet-stream";
    } else {
      return "application/zip";
    }
  }
}
