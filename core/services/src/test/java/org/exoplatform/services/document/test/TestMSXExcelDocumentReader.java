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
package org.exoplatform.services.document.test;

import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.document.impl.MSXExcelDocumentReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.xml.sax.SAXParseException;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko
 * <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestMSXExcelDocumentReader extends BaseStandaloneTest
{
  private static final Log  LOG = ExoLogger.getLogger(TestMSXExcelDocumentReader.class);

   DocumentReaderServiceImpl service;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      service.addDocumentReader(new MSXExcelDocumentReader());
   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = TestMSXExcelDocumentReader.class.getResourceAsStream("/test.xlsx");
      try
      {
         String text =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
               .getContentAsText(is);

         String expected =
            "Sheet2 "
               +"Ronaldo Eric Cantona Kaka Ronaldonho "
               +" Sheet1 "
               + "Group Functionality Executor Begin End Tested "
               + "XNNL XNNL Xay dung vung quan li nguyen lieu NamPH Tested "
               + "XNNL XNNL XNNL_HAVEST NamPH Tested "
               + "XNNL XNNL XNNL_PIECE_OF_GROUND NamPH Tested "
               + "XNNL XNNL XNNL_76 NamPH "
               + "XNNL XNNL XNNL_CREATE_REAP NamPH none "
               + "XNNL XNNL XNNL_SCALE NamPH Tested "
               + "XNNL XNNL LASUCO_PROJECT NamPH "
               + "XNNL XNNL LASUCO_PROJECT NamPH Tested "
               + "XNNL XNNL XNNL_BRANCH NamPH Tested "
               + "XNNL XNNL XNNL_SUGAR_RACE NamPH "
               + "XNNL XNNL F_XNNL_DISTRI NamPH Tested "
               + "XNNL XNNL XNNL_LASUCO_USER NamPH ";

         assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXXE() throws Exception
   {
      InputStream is = TestMSXExcelDocumentReader.class.getResourceAsStream("/test.xlsx");
      file = createTempFile("test", ".xlsx");
      replaceFirstInZip(
         is,
         file,
         "xl/sharedStrings.xml",
         "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"72\" uniqueCount=\"72\"><si><t>",
         "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
            + TestMSXExcelDocumentReader.class.getResource("/test.txt")
            + "\">]>"
            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"72\" uniqueCount=\"72\"><si><t>&xxe;");
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
               .getContentAsText(is);

        fail("XXE are not allowed and must generate an error");
      } catch (DocumentReadException e) {
        LOG.info("Document was not read due to XXE prevention");
      } finally {
         is.close();
      }
   }

   /**
    * test XXE External Entity point to non-existing resource
    */
   
   public void testGetContentAsStringXXE2() throws Exception
   {
      InputStream is = TestMSXExcelDocumentReader.class.getResourceAsStream("/test.xlsx");
      file = createTempFile("test", ".xlsx");
      replaceFirstInZip(
         is,
         file,
         "xl/sharedStrings.xml",
         "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"72\" uniqueCount=\"72\"><si><t>",
         "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
            + TestMSXExcelDocumentReader.class.getResource("/test123.txt")
            + "\">]>"
            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"72\" uniqueCount=\"72\"><si><t>&xxe;");
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
               .getContentAsText(is);


        fail("XXE are not allowed and must generate an error");
      } catch (DocumentReadException e) {
        LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }

   public void testGetContentAsStringXEE() throws Exception
   {
      InputStream is = TestMSXExcelDocumentReader.class.getResourceAsStream("/test.xlsx");
      file = createTempFile("test", ".xlsx");
      replaceFirstInZip(
         is,
         file,
         "xl/sharedStrings.xml",
         "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"72\" uniqueCount=\"72\"><si><t>",
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
         + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
         + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
         + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
         + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
         + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
         + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>"
            + "<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"72\" uniqueCount=\"72\"><si><t>&xee6;");
      is = new FileInputStream(file);
      try
      {
         service.getDocumentReader("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .getContentAsText(is);

         fail("An exception is expected");
      }
      catch (DocumentReadException e)
      {
        LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }
}
