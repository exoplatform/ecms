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

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.document.DCMetaData;
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.DocumentReader;
import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.document.impl.HTMLDocumentReader;
import org.exoplatform.services.document.impl.MSExcelDocumentReader;
import org.exoplatform.services.document.impl.MSOutlookDocumentReader;
import org.exoplatform.services.document.impl.MSWordDocumentReader;
import org.exoplatform.services.document.impl.MSXExcelDocumentReader;
import org.exoplatform.services.document.impl.MSXPPTDocumentReader;
import org.exoplatform.services.document.impl.MSXWordDocumentReader;
import org.exoplatform.services.document.impl.OpenOfficeDocumentReader;
import org.exoplatform.services.document.impl.PDFDocumentReader;
import org.exoplatform.services.document.impl.PPTDocumentReader;
import org.exoplatform.services.document.impl.TextPlainDocumentReader;
import org.exoplatform.services.document.impl.XMLDocumentReader;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

public class TestPropertiesExtracting extends BaseStandaloneTest
{
   private static final Log LOG = ExoLogger.getLogger(TestPropertiesExtracting.class);

   DocumentReaderServiceImpl service;

   @Override
   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      InitParams params = new InitParams();
      service.addDocumentReader(new TextPlainDocumentReader(params));
      service.addDocumentReader(new XMLDocumentReader());
      service.addDocumentReader(new HTMLDocumentReader());
      service.addDocumentReader(new MSExcelDocumentReader());
      service.addDocumentReader(new MSOutlookDocumentReader());
      service.addDocumentReader(new MSWordDocumentReader());
      service.addDocumentReader(new MSXExcelDocumentReader());
      service.addDocumentReader(new MSXPPTDocumentReader());
      service.addDocumentReader(new MSXWordDocumentReader());
      service.addDocumentReader(new OpenOfficeDocumentReader());
      service.addDocumentReader(new PDFDocumentReader());
      service.addDocumentReader(new PPTDocumentReader());
   }

   public void testPDFDocumentReaderServiceXMPMetadata() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/pfs_accapp.pdf");
      try
      {
         DocumentReader rdr = service.getDocumentReader("application/pdf");
         Properties testprops = rdr.getProperties(is);
         Properties etalon = new Properties();
         etalon.put(DCMetaData.TITLE, "Personal Account Opening Form VN");
         etalon.put(DCMetaData.CREATOR, "mr");
         etalon.put(DCMetaData.PUBLISHER, "Adobe LiveCycle Designer ES 8.2");
         evalProps(etalon, testprops, false);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testPDFDocumentReaderServiceBrokenFile() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/pfs_accapp.pdf");
      try
      {

         DocumentReader rdr = service.getDocumentReader("application/pdf");
         Properties testprops = rdr.getProperties(is);
         Properties etalon = new Properties();
         etalon.put(DCMetaData.TITLE, "Personal Account Opening Form VN");
         etalon.put(DCMetaData.CREATOR, "mr");
         etalon.put(DCMetaData.PUBLISHER, "Adobe LiveCycle Designer ES 8.2");
         evalProps(etalon, testprops, false);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testPDFDocumentReaderServiceXMPUsecase1() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/Trait_union.06.Mai_2009.pdf");
      try
      {
         DocumentReader rdr = service.getDocumentReader("application/pdf");
         Properties testprops = rdr.getProperties(is);
         Properties etalon = new Properties();
         etalon.put(DCMetaData.TITLE, "journal interne mai 2009.qxp");
         etalon.put(DCMetaData.CREATOR, "presse");
         evalProps(etalon, testprops, false);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testWordDocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.doc");
      try
      {
         Properties props = service.getDocumentReader("application/msword").getProperties(is);
         Properties etalon = new Properties();
         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.DATE, new Date(1283247060000L));
         etalon.put(DCMetaData.SUBJECT, "test-Subject");
         etalon.put(DCMetaData.CREATOR, "Max Yakimenko");
         etalon.put(DCMetaData.CONTRIBUTOR, "Max Yakimenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");
         evalProps(etalon, props, true);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testPPTDocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.ppt");
      try
      {
         Properties props = service.getDocumentReader("application/powerpoint").getProperties(is);
         Properties etalon = new Properties();
         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.DATE, new Date(1662112998853L));
         etalon.put(DCMetaData.SUBJECT, "test-Subject");
         etalon.put(DCMetaData.CREATOR, "Max Yakimenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");
         evalProps(etalon, props, false);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testExcelDocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.xls");
      try
      {
         Properties props = service.getDocumentReader("application/excel").getProperties(is);
         Properties etalon = new Properties();
         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.DATE, new Date(1283247293000L));
         etalon.put(DCMetaData.SUBJECT, "test-Subject");
         etalon.put(DCMetaData.CREATOR, "KHANH NGUYEN GIA");
         etalon.put(DCMetaData.CONTRIBUTOR, "Max Yakimenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");

         evalProps(etalon, props, true);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testXWordDocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.docx");
      try
      {
         Properties props =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
               .getProperties(is);
         Properties etalon = new Properties();
         Calendar date = Calendar.getInstance();
         date.setTimeInMillis(0);
         date.set(2010, 7, 31, 7, 53, 0);

         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.DATE, date.getTime());
         etalon.put(DCMetaData.SUBJECT, "Subject");
         etalon.put(DCMetaData.CREATOR, "nikolaz");
         etalon.put(DCMetaData.CONTRIBUTOR, "Max Yakimenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");

         evalProps(etalon, props, true);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }
   
   public void testXWordDocumentReaderServiceXEE() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.docx");
      file = createTempFile("test", ".docx");
      replaceFirstInZip(is, file, "docProps/core.xml", new String[]{"<cp:coreProperties", "<dc:title>"}, new String[]{
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>" + "<cp:coreProperties",
         "<dc:title>&xee6;"});
      is = new FileInputStream(file);
      try
      {
         Properties props =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
               .getProperties(is);
         assertTrue(props.isEmpty());
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testXPPTDocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.pptx");
      try
      {
         Properties props =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
               .getProperties(is);
         Properties etalon = new Properties();

         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.DATE, "2010-08-31T07:59:37Z");
         etalon.put(DCMetaData.SUBJECT, "test-Subject");
         etalon.put(DCMetaData.CREATOR, "Max Yakimenko");
         etalon.put(DCMetaData.CONTRIBUTOR, "Max Yakimenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");

         evalProps(etalon, props, true);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }
   
   /**
    * test XXE External Entity point to non-existing resource
    */
   
   public void testXPPTDocumentReaderServiceXXE2() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.pptx");
      file = createTempFile("test", ".pptx");
      replaceFirstInZip(
         is,
         file,
         "docProps/core.xml",
         new String[]{"<cp:coreProperties", "<dc:title>"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestPropertiesExtracting.class.getResource("/test123.txt") + "\">]><cp:coreProperties", "<dc:title>&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
               .getProperties(is);
        fail("XXE are not allowed and must generate an error");
      } catch (DocumentReadException e) {
        LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }
   
   public void testXPPTDocumentReaderServiceXEE() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.pptx");
      file = createTempFile("test", ".pptx");
      replaceFirstInZip(is, file, "docProps/core.xml", new String[]{"<cp:coreProperties", "<dc:title>"}, new String[]{
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>" + "<cp:coreProperties",
         "<dc:title>&xee6;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.openxmlformats-officedocument.presentationml.presentation")
               .getProperties(is);
        fail("XXE are not allowed and must generate an error");
      } catch (DocumentReadException e) {
        LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }

   public void testXExcelDocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.xlsx");
      try
      {
         Properties props =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
               .getProperties(is);
         Properties etalon = new Properties();
         Calendar date = Calendar.getInstance();
         date.setTimeInMillis(0);
         date.set(2010, 7, 31, 8, 7, 25);

         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.DATE, date.getTime());
         etalon.put(DCMetaData.SUBJECT, "test-Subject");
         etalon.put(DCMetaData.CREATOR, "KHANH NGUYEN GIA");
         etalon.put(DCMetaData.CONTRIBUTOR, "Max Yakimenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");

         evalProps(etalon, props, true);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }
   
   public void testXExcelDocumentReaderServiceXEE() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.xlsx");
      file = createTempFile("test", ".xlsx");
      replaceFirstInZip(
         is,
         file,
         "docProps/core.xml",
         "<cp:coreProperties .*<dc:title>",
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>"
            + "<cp:coreProperties xmlns:cp=\"http://schemas.openxmlformats.org/package/2006/metadata/core-properties\" "
            + "xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:dcterms=\"http://purl.org/dc/terms/\" "
            + "xmlns:dcmitype=\"http://purl.org/dc/dcmitype/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"><dc:title>&xee6;");
      is = new FileInputStream(file);
      try
      {
         Properties props =
            service.getDocumentReader("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
               .getProperties(is);
         assertTrue(props.isEmpty());
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testOODocumentReaderService() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.odt");
      try
      {
         Properties props = service.getDocumentReader("application/vnd.oasis.opendocument.text").getProperties(is);
         Properties etalon = new Properties();

         etalon.put(DCMetaData.TITLE, "test-Title");
         etalon.put(DCMetaData.LANGUAGE, "ru-RU");
         etalon.put(DCMetaData.DATE, "2010-09-03T14:37:59.10");
         etalon.put(DCMetaData.SUBJECT, "test-Subject");
         etalon.put(DCMetaData.CREATOR, "Sergiy Karpenko");
         etalon.put(DCMetaData.DESCRIPTION, "test-Comments");

         evalProps(etalon, props, true);
      } catch (Exception e) {
        LOG.error("Error reading document properties", e);
        fail();
      }
      finally
      {
         is.close();
      }
   }

   public void testOODocumentReaderServiceXXE() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(
         is,
         file,
         "meta.xml",
         new String[]{"<office:document-meta", "<dc:description>"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestPropertiesExtracting.class.getResource("/test.txt") + "\">]>" + "<office:document-meta",
            "<dc:description>&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.oasis.opendocument.text").getProperties(is);
        fail("XXE are not allowed and must generate an error");
      } catch (DocumentReadException e) {
        LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }

   /**
    * test XXE External Entity point to non-existing resource
    */
   
   public void testOODocumentReaderServiceXXE2() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(
         is,
         file,
         "meta.xml",
         new String[]{"<office:document-meta", "<dc:description>"},
         new String[]{
            "<!DOCTYPE foo [<!ELEMENT foo ANY ><!ENTITY xxe SYSTEM \""
               + TestPropertiesExtracting.class.getResource("/test123.txt") + "\">]>" + "<office:document-meta",
            "<dc:description>&xxe;"});
      is = new FileInputStream(file);
      try
      {
        service.getDocumentReader("application/vnd.oasis.opendocument.text").getProperties(is);
        fail("XXE are not allowed and must generate an error");
      } catch (DocumentReadException e) {
        LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }
   
   public void testOODocumentReaderServiceXEE() throws Exception
   {
      InputStream is = TestPropertiesExtracting.class.getResourceAsStream("/test.odt");
      file = createTempFile("test", ".odt");
      replaceFirstInZip(is, file, "meta.xml", new String[]{"<office:document-meta", "<dc:description>"}, new String[]{
         "<!DOCTYPE lolz [<!ENTITY xee \"xee\">"
            + "<!ENTITY xee1 \"&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;&xee;\">"
            + "<!ENTITY xee2 \"&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;&xee1;\">"
            + "<!ENTITY xee3 \"&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;&xee2;\">"
            + "<!ENTITY xee4 \"&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;&xee3;\">"
            + "<!ENTITY xee5 \"&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;&xee4;\">"
            + "<!ENTITY xee6 \"&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;&xee5;\">]>"
            + "<office:document-meta", "<dc:description>&xee6;"});
      is = new FileInputStream(file);
      try
      {
         service.getDocumentReader("application/vnd.oasis.opendocument.text").getProperties(is);
         fail("XXE are not allowed and must generate an error");
       } catch (DocumentReadException e) {
         LOG.info("Document was not read due to XXE prevention");
      }
      finally
      {
         is.close();
      }
   }

   private void evalProps(Properties etalon, Properties testedProps, boolean testSize)
   {
      Iterator it = etalon.entrySet().iterator();
      while (it.hasNext())
      {
         Map.Entry prop = (Map.Entry)it.next();
         Object tval = testedProps.get(prop.getKey());
         assertNotNull(prop.getKey() + " property not founded. ", tval);
         assertEquals(prop.getKey() + " property value is incorrect", prop.getValue(), tval);
      }
      if (testSize)
      {
         assertEquals("size is incorrect", etalon.size(), testedProps.size());
      }
   }

}
