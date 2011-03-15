/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.cms.thumbnail.impl;

import java.awt.image.BufferedImage;
import java.util.List;

import javax.imageio.ImageIO;
import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.thumbnail.ThumbnailPlugin;

/**
 * Created by The eXo Platform SARL
 * Author : Dang Van Minh
 *          minh.dang@exoplatform.com
 * Oct 22, 2009
 * 2:20:20 PM
 */
public class ImageThumbnailPlugin implements ComponentPlugin, ThumbnailPlugin{

  private ThumbnailType config;
  private String description;
  private String name;

  public ImageThumbnailPlugin(InitParams initParams) throws Exception {
    config = initParams.getObjectParamValues(ThumbnailType.class).get(0);
  }

  public String getDescription() {
    return description;
  }

  public String getName() {
    return name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setName(String name) {
    this.name = name;
  }

  public BufferedImage getBufferedImage(Node contentNode, String nodePath) throws Exception {
    return ImageIO.read(contentNode.getProperty("jcr:data").getStream());
  }

  public List<String> getMimeTypes() {
    return config.getMimeTypes();
  }

}
