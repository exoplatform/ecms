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

import org.exoplatform.services.document.impl.DocumentReaderServiceImpl;
import org.exoplatform.services.document.impl.PPTDocumentReader;

import java.io.InputStream;

/**
 * Created by The eXo Platform SAS Author : Sergey Karpenko
 * <sergey.karpenko@exoplatform.com.ua>
 * 
 * @version $Id: $
 */

public class TestPPTDocumentReader extends BaseStandaloneTest
{
   DocumentReaderServiceImpl service;

   public void setUp() throws Exception
   {
      super.setUp();
      service = new DocumentReaderServiceImpl(null);
      service.addDocumentReader(new PPTDocumentReader());
   }

   public void testGetContentAsString() throws Exception
   {
      InputStream is = TestPPTDocumentReader.class.getResourceAsStream("/test.ppt");
      try
      {
         String text = service.getDocumentReader("application/powerpoint").getContentAsText(is);
         String etalon =
                       "TEST POWERPOINT\n" + "Manchester United\n\n" + "AC Milan\n" + "SLIDE 2\n" + "Eric Cantona\n\n"
                           + "Kaka\n\n" + "Ronaldo\n\n" + "The natural scients universitys\n";
         assertEquals("Wrong string returned", etalon, text);
      }
      finally
      {
         is.close();
      }
   }
}
