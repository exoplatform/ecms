/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.AccessControlException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.transform.dom.DOMSource;

import org.junit.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.w3c.dom.Element;

import org.exoplatform.ecm.connector.fckeditor.FCKMessage;
import org.exoplatform.services.jcr.access.PermissionType;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

public class FileUploadHandlerTest {

  private static final String UPLOAD_ID = "uploadId";

  private Response save(UploadService uploadService, ExtendedNode parent, String existenceAction) throws Exception {
    try (MockedStatic<WCMCoreUtils> utils = Mockito.mockStatic(WCMCoreUtils.class)) {
      utils.when(() -> WCMCoreUtils.getService(UploadService.class)).thenReturn(uploadService);
      FileUploadHandler handler = new FileUploadHandler();
      return handler.saveAsNTFile("collaboration",
                                  parent,
                                  UPLOAD_ID,
                                  "file.txt",
                                  "en",
                                  "source",
                                  "siteName",
                                  "userId",
                                  existenceAction,
                                  false);
    }
  }

  private void assertMessage(Response response, int number) {
    assertNotNull(response);
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.TEXT_XML, response.getMetadata().getFirst("Content-Type"));
    Element message = ((org.w3c.dom.Document) ((DOMSource) response.getEntity()).getNode()).getDocumentElement();
    assertEquals(String.valueOf(number), message.getAttribute("number"));
    assertEquals(FCKMessage.ERROR, message.getAttribute("type"));
    assertTrue(message.getAttribute("text").length() > 0);
  }

  @Test
  public void testSaveAsNTFileWithUnknownUploadId() throws Exception {
    UploadService uploadService = mock(UploadService.class);
    when(uploadService.getUploadResource(UPLOAD_ID)).thenReturn(null);
    ExtendedNode parent = mock(ExtendedNode.class); // add permission granted

    Response response = save(uploadService, parent, "keep");

    assertMessage(response, FCKMessage.FILE_NOT_UPLOADED);
    verify(uploadService).getUploadResource(UPLOAD_ID);
  }

  @Test
  public void testSaveAsNTFileWithUnknownUploadIdDoesNotReplaceExistingFile() throws Exception {
    UploadService uploadService = mock(UploadService.class);
    when(uploadService.getUploadResource(UPLOAD_ID)).thenReturn(null);
    ExtendedNode parent = mock(ExtendedNode.class);
    when(parent.hasNode(anyString())).thenReturn(true);

    Response response = save(uploadService, parent, FileUploadHandler.REPLACE);

    assertMessage(response, FCKMessage.FILE_NOT_UPLOADED);
    verify(parent, never()).getNode(anyString());
  }

  @Test
  public void testSaveAsNTFileWithExistingUploadIsNotRefusedAsMissing() throws Exception {
    UploadService uploadService = mock(UploadService.class);
    when(uploadService.getUploadResource(UPLOAD_ID)).thenReturn(mock(UploadResource.class));
    ExtendedNode parent = mock(ExtendedNode.class);
    doThrow(new AccessControlException("denied")).when(parent).checkPermission(PermissionType.ADD_NODE);
    when(parent.getPath()).thenReturn("/parent");

    Response response = save(uploadService, parent, "keep");

    assertMessage(response, FCKMessage.FILE_UPLOAD_RESTRICTION);
  }
}
