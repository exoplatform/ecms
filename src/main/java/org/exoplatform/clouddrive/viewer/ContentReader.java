
/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
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
 * An access to Cloud File content connected with particular drive instance.<br>
 * 
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: ContentReader.java 00000 Nov 19, 2014 pnedonosko $
 * 
 */
public interface ContentReader {

  /**
   * Content stream for reading.
   * 
   * @return {@link InputStream}
   */
  InputStream getStream();
  
  /**
   * Content type in MIME format.
   * 
   * @return {@link String}
   */
  String getMimeType();
  
  /**
   * Optional representation (UI) mode for this content type. Can be <code>null</code>.
   * 
   * @return {@link String} a type mode or <code>null</code> if not available.
   */
  String getTypeMode();
  
  /**
   * Content length in bytes.
   * 
   * @return {@link Long}
   */
  long getLength();
}
