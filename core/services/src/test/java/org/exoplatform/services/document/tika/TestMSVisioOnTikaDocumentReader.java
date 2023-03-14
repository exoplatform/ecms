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
 * @author <a href="aboughzela@exoplatform.com">Aymen Boughzela</a>
 * @version $Id: TestMSVisioOnTikaDocumentReader.java
 */
public class TestMSVisioOnTikaDocumentReader  extends BaseStandaloneTest
{
    DocumentReaderService service;

    public void setUp() throws Exception
    {
        super.setUp();
        service = (DocumentReaderService)getComponentInstanceOfType(DocumentReaderService.class);
    }

    public void testGetContentAsStringTemplate() throws Exception
    {
        InputStream is = BaseStandaloneTest.class.getResourceAsStream("/test.vsd");
        String text = service.getDocumentReader("application/vnd.visio").getContentAsText(is);
        String expected = "My first test with visio";
        assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
    }

    public void testGetContentAsReader() throws Exception
    {
        InputStream is = TestPPTOnTikaDocumentReader.class.getResourceAsStream("/test.vsd");
        try
        {
            Reader reader =
                    ((AdvancedDocumentReader)service.getDocumentReader("application/vnd.visio")).getContentAsReader(is);

            //read text
            StringBuffer buf = new StringBuffer();
            int c;
            while ((c = reader.read()) != -1)
            {
                char ch = (char)c;
                buf.append(ch);
            }

            String text = buf.toString();
            String expected = "My first test with visio";

            assertEquals("Wrong string returned", normalizeWhitespaces(expected), normalizeWhitespaces(text));
        }
        finally
        {
            is.close();
        }
    }

}
