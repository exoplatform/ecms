
/*
 * Copyright (C) 2003-2018 eXo Platform SAS.
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
package org.exoplatform.clouddrive.viewer;

import java.io.InputStream;

/**
 * Cloud File content for reading locally.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudFileContent.java 00000 Jul 26, 2015 pnedonosko $
 */
public class CloudFileContent implements ContentReader {

  /** The content. */
  protected final InputStream content;

  /** The type. */
  protected final String      type;

  /** The type mode. */
  protected final String      typeMode;

  /** The length. */
  protected final long        length;

  /**
   * Instantiates a new cloud file content.
   *
   * @param content the content
   * @param type the type
   * @param typeMode the type mode
   * @param length the length
   */
  public CloudFileContent(InputStream content, String type, String typeMode, long length) {
    this.content = content;
    this.length = length;
    this.type = type;
    this.typeMode = typeMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public InputStream getStream() {
    return content;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getMimeType() {
    return type;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getTypeMode() {
    return typeMode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public long getLength() {
    return length;
  }
}
