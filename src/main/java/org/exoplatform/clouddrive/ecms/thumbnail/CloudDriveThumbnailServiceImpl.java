/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.clouddrive.ecms.thumbnail;

import org.exoplatform.clouddrive.CloudDriveService;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.thumbnail.impl.ThumbnailServiceImpl;

import java.awt.image.BufferedImage;
import java.io.InputStream;

import javax.jcr.Node;

/**
 * TODO not finished! not used.
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveThumbnailServiceImpl.java 00000 May 21, 2014 pnedonosko $
 * 
 */
public class CloudDriveThumbnailServiceImpl extends ThumbnailServiceImpl {

  protected final CloudDriveService cloudDrives;

  /**
   * @param initParams
   * @throws Exception
   */
  public CloudDriveThumbnailServiceImpl(InitParams initParams, CloudDriveService cloudDrives) throws Exception {
    super(initParams);
    this.cloudDrives = cloudDrives;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getThumbnailImage(Node node, String thumbnailType) throws Exception {
    // TODO Auto-generated method stub
    return super.getThumbnailImage(node, thumbnailType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addThumbnailImage(Node thumbnailNode, BufferedImage image, String propertyName) throws Exception {
    // TODO Auto-generated method stub
    super.addThumbnailImage(thumbnailNode, image, propertyName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createSpecifiedThumbnail(Node node, BufferedImage image, String propertyName) throws Exception {
    // TODO Auto-generated method stub
    super.createSpecifiedThumbnail(node, image, propertyName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createThumbnailImage(Node node, BufferedImage image, String mimeType) throws Exception {
    // TODO Auto-generated method stub
    super.createThumbnailImage(node, image, mimeType);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node addThumbnailNode(Node node) throws Exception {
    // TODO Auto-generated method stub
    return super.addThumbnailNode(node);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Node getThumbnailNode(Node node) throws Exception {
    // return null for Cloud Drive files
    if (cloudDrives.findDrive(node) != null) {
      return null;
    } else {
      return super.getThumbnailNode(node);
    }
  }

}
