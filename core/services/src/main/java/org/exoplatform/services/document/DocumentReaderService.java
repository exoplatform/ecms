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

import java.io.InputStream;

/**
 * Created by The eXo Platform SAS
 * 
 * @author Gennady Azarenkov
 * @version $Id: DocumentReaderService.java 11659 2007-01-05 15:35:06Z geaz $
 */
public interface DocumentReaderService
{

   /**
    * @deprecated
    */
   String getContentAsText(String mimeType, InputStream is) throws Exception;

   /**
    * @param mimeType
    * @return appropriate document reader
    */
   DocumentReader getDocumentReader(String mimeType) throws HandlerNotFoundException;

}
