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
package org.exoplatform.services.document.tika;

import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.HandlerNotFoundException;
import org.exoplatform.services.document.impl.BaseDocumentReader;
import org.exoplatform.services.document.impl.tika.TikaDocumentReader;
import org.exoplatform.services.document.impl.tika.TikaDocumentReaderServiceImpl;
import org.exoplatform.services.document.test.BaseStandaloneTest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMimetypes.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMimetypes extends BaseStandaloneTest
{
   TikaDocumentReaderServiceImpl drs;

   private class FakeDocumentReader extends BaseDocumentReader
   {

      public String getContentAsText(InputStream is) throws IOException, DocumentReadException
      {
         return null;
      }

      public String getContentAsText(InputStream is, String encoding) throws IOException, DocumentReadException
      {
         return null;
      }

      public String[] getMimeTypes()
      {
         return new String[]{"application/fake"};
      }

      public Properties getProperties(InputStream is) throws IOException, DocumentReadException
      {
         return null;
      }
   }

   public void setUp() throws Exception
   {
      super.setUp();
      drs = (TikaDocumentReaderServiceImpl)getComponentInstanceOfType(DocumentReaderService.class);
   }

   /**
    * This text check does DocumentReader service returns old-style DocumentReader registered by user
    * (in this case PDFDocumentReader).
    */
   public void testRegisteredMimetype() throws Exception
   {
      drs.addDocumentReader(new FakeDocumentReader());

      try
      {
         assertTrue((drs.getDocumentReader("application/fake") instanceof FakeDocumentReader));
      }
      catch (HandlerNotFoundException e)
      {
         fail(e.getMessage());
      }

      try
      {
         assertTrue((drs.getDocumentReader("application/xls") instanceof TikaDocumentReader));
      }
      catch (HandlerNotFoundException e)
      {
         fail(e.getMessage());
      }
   }

   /**
    * This test check does all mimetypes inherited from previous versions registered in TikaDocumentReader.
    */
   public void testDefaultMimetypes()
   {
      String[] mimetypes =
         new String[]{"text/html", "application/excel", "application/xls", "application/vnd.ms-outlook",
            "application/msword", "application/msworddoc", "application/msworddot",
            "application/vnd.oasis.opendocument.database", "application/vnd.oasis.opendocument.formula",
            "application/vnd.oasis.opendocument.graphics", "application/vnd.oasis.opendocument.presentation",
            "application/vnd.oasis.opendocument.spreadsheet", "application/vnd.oasis.opendocument.text",
            "application/pdf", "application/powerpoint", "application/ppt", "text/plain", "script/groovy",
            "application/x-groovy", "application/x-javascript", "application/javascript", "text/javascript",
            "text/xml", "application/xml", "application/x-google-gadget",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};

      for (int i = 0; i < mimetypes.length; i++)
      {
         try
         {
            assertTrue((drs.getDocumentReader(mimetypes[i]) instanceof TikaDocumentReader));
         }
         catch (HandlerNotFoundException e)
         {
            fail(e.getMessage());
         }
      }
   }
}
