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
import org.exoplatform.services.document.DocumentReadException;
import org.exoplatform.services.document.DocumentReader;
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

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SAS.
 * 
 * <br>Date:
 *
 * @author <a href="karpenko.sergiy@gmail.com">Karpenko Sergiy</a> 
 * @version $Id: TestDocumentReadersIncomVals.java 111 2008-11-11 11:11:11Z serg $
 */
public class TestDocumentReadersIncomVals extends BaseStandaloneTest
{
   List<DocumentReader> serviceList;

   private static final Log LOG = ExoLogger.getLogger(TestDocumentReadersIncomVals.class);

   public TestDocumentReadersIncomVals()
   {
      serviceList = new ArrayList<DocumentReader>();
      serviceList.add(new HTMLDocumentReader());
      serviceList.add(new MSExcelDocumentReader());
      serviceList.add(new MSXExcelDocumentReader());
      serviceList.add(new MSOutlookDocumentReader());
      serviceList.add(new MSWordDocumentReader());
      serviceList.add(new MSXWordDocumentReader());
      serviceList.add(new OpenOfficeDocumentReader());
      serviceList.add(new PDFDocumentReader());
      serviceList.add(new PPTDocumentReader());
      serviceList.add(new MSXPPTDocumentReader());
      serviceList.add(new TextPlainDocumentReader(new InitParams()));
      serviceList.add(new XMLDocumentReader());
   }

   public void testNull() throws Exception
   {

      StringBuilder sb = new StringBuilder();
      //List<String> fails = new ArrayList<String>();
      for (int i = 0; i < serviceList.size(); i++)
      {
         try
         {
            serviceList.get(i).getContentAsText(null);
            sb.append(serviceList.get(i).toString() + "\n");
            //  fails.add(serviceList.get(i).toString());
         }
         catch (IllegalArgumentException e)
         {
            //ok
         }
         catch (Exception e)
         {
            sb.append(serviceList.get(i).toString() + " " + e.getMessage());
            e.printStackTrace();
         }
      }

      if (sb.length() != 0)
      {
         fail(sb.toString());
         System.out.println(sb.toString());
      }
   }

   public void testEmptyStream() throws Exception
   {
      StringBuilder sb = new StringBuilder();

      File f = createTempFile("dfd", "suf");
      createNewFile(f);
      InputStream in;

      for (int i = 0; i < serviceList.size(); i++)
      {
         in = getInputStream(f);
         try
         {
            assertEquals("", serviceList.get(i).getContentAsText(in));
         }
         catch (DocumentReadException e) {
           LOG.info("Unable to read Document");
         }
         catch (Exception e)
         {
            sb.append(serviceList.get(i).toString() + " " + e.getMessage() + "\n");
            e.printStackTrace();
         }
         in.close();
      }

      if (sb.length() != 0)
      {
         fail(sb.toString());
         System.out.println(sb.toString());
      }

      deleteFile(f);
   }
}
