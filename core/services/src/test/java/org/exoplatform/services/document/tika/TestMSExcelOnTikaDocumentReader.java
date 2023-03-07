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

import org.exoplatform.services.document.AdvancedDocumentReader;
import org.exoplatform.services.document.DocumentReaderService;
import org.exoplatform.services.document.test.BaseStandaloneTest;

import java.io.InputStream;
import java.io.Reader;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestMSExcelOnTikaDocumentReader.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestMSExcelOnTikaDocumentReader extends BaseStandaloneTest
{
   DocumentReaderService service;

   public void setUp() throws Exception
   {
      super.setUp();
      service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);
   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.xls");
      try
      {
         String text = service.getDocumentReader("application/excel").getContentAsText(is);

         String expected =
            "Sheet2 Ronaldo Eric Cantona Kaka Ronaldonho &C&\"Times New Roman,Regular\"&12&A "
               + "&C&\"Times New Roman,Regular\"&12Page &P Sheet1 ID Group Functionality Executor Begin End "
               + "Tested XNNL XNNL Xay dung vung quan li nguyen lieu NamPH 2/2/05 10/02/2005 "
               + "Tested XNNL XNNL XNNL_HAVEST NamPH 1223554 10/01/2005 "
               + "Tested XNNL XNNL XNNL_PIECE_OF_GROUND NamPH 10/12/05 10/02/2005 "
               + "Tested XNNL XNNL XNNL_76 NamPH 0 12/10/84 No XNNL XNNL XNNL_CREATE_REAP NamPH none 10/03/2005 No XNNL XNNL XNNL_SCALE NamPH 12/10/84 10/05/2005 "
               + "Tested XNNL XNNL LASUCO_PROJECT NamPH 10/05/05 10/06/2005 No XNNL XNNL LASUCO_PROJECT NamPH "
               + "Tested XNNL XNNL XNNL_BRANCH NamPH 12/12/05 06/10/2005 "
               + "Tested XNNL XNNL XNNL_SUGAR_RACE NamPH 05/09/05 06/10/2005 No XNNL XNNL F_XNNL_DISTRI NamPH 05/09/05 06/10/2005 "
               + "Tested XNNL XNNL XNNL_LASUCO_USER NamPH 09/09/05 06/10/2005 No";

         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsReader() throws Exception
   {
      InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.xls");
      try
      {
         Reader reader =
            ((AdvancedDocumentReader)service.getDocumentReader("application/excel")).getContentAsReader(is);

         //read text
         StringBuffer buf = new StringBuffer();
         int c;
         while ((c = reader.read()) != -1)
         {
            char ch = (char)c;
            buf.append(ch);
         }

         String text = buf.toString();
         String expected =
             "Sheet2 Ronaldo Eric Cantona Kaka Ronaldonho &C&\"Times New Roman,Regular\"&12&A "
               + "&C&\"Times New Roman,Regular\"&12Page &P Sheet1 ID Group Functionality Executor Begin End "
               + "Tested XNNL XNNL Xay dung vung quan li nguyen lieu NamPH 2/2/05 10/02/2005 "
               + "Tested XNNL XNNL XNNL_HAVEST NamPH 1223554 10/01/2005 "
               + "Tested XNNL XNNL XNNL_PIECE_OF_GROUND NamPH 10/12/05 10/02/2005 "
               + "Tested XNNL XNNL XNNL_76 NamPH 0 12/10/84 No XNNL XNNL XNNL_CREATE_REAP NamPH none 10/03/2005 No XNNL XNNL XNNL_SCALE NamPH 12/10/84 10/05/2005 "
               + "Tested XNNL XNNL LASUCO_PROJECT NamPH 10/05/05 10/06/2005 No XNNL XNNL LASUCO_PROJECT NamPH "
               + "Tested XNNL XNNL XNNL_BRANCH NamPH 12/12/05 06/10/2005 "
               + "Tested XNNL XNNL XNNL_SUGAR_RACE NamPH 05/09/05 06/10/2005 No XNNL XNNL F_XNNL_DISTRI NamPH 05/09/05 06/10/2005 "
               + "Tested XNNL XNNL XNNL_LASUCO_USER NamPH 09/09/05 06/10/2005 No";

         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }
}
