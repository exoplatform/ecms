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
package org.exoplatform.services.document;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS Author : Nam
 * 
 * @author Gennady Azarenkov
 * @version $Id: DocumentReader.java 11659 2007-01-05 15:35:06Z geaz $
 */
public interface DocumentReader
{

   /**
    * @return all appropriate mime types
    */
   String[] getMimeTypes();

   /**
    * @param is
    * @return document content
    * @throws IOException
    * @throws DocumentReadException
    */
   String getContentAsText(InputStream is) throws IOException, DocumentReadException;

   /**
    * @param is data input stream
    * @param encoding char set for input stream
    * @return document content
    * @throws IOException
    * @throws DocumentReadException
    */
   String getContentAsText(InputStream is, String encoding) throws IOException, DocumentReadException;

   /**
    * @param is
    * @return metainfo properties reduced to some supported metadata set (Dublin
    *         Core or other)
    * @throws IOException
    * @throws DocumentReadException
    */
   Properties getProperties(InputStream is) throws IOException, DocumentReadException;
}