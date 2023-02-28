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
import java.io.Reader;

/**
 * This Interface extends DocumentReader with two getContentAsReader methods. 
 * 
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 * * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: AdvancedDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public interface AdvancedDocumentReader extends DocumentReader
{
   /**
    * Return text content from stream as reader.
    * 
    * @param is - input stream 
    * @param encoding - encoding of text content
    * @return
    * @throws IOException
    * @throws DocumentReadException
    */
   Reader getContentAsReader(InputStream is, String encoding) throws IOException, DocumentReadException;

   /**
    * Return text content from stream as reader.
    * 
    * @param is - input stream 
    * @return
    * @throws IOException
    * @throws DocumentReadException
    */
   Reader getContentAsReader(InputStream is) throws IOException, DocumentReadException;
}
