/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <gnu.org/licenses>.
 */
package org.exoplatform.wcm.connector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import org.apache.commons.fileupload2.core.FileUploadException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.cms.documents.DocumentService;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.upload.UploadService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * The upload control's abort and delete act as the request's user: the
 * removal takes the request, and a refusal answers 400 with its reason
 */
@RunWith(MockitoJUnitRunner.class)
public class FileUploadHandlerControlTest {

  private MockedStatic<WCMCoreUtils> wcmCoreUtils;

  private UploadService              uploadService;

  private HttpServletRequest         request;

  private FileUploadHandler          handler;

  @Before
  public void setUp() {
    uploadService = mock(UploadService.class);
    request = mock(HttpServletRequest.class);
    wcmCoreUtils = mockStatic(WCMCoreUtils.class);
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(UploadService.class)).thenReturn(uploadService);
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(ListenerService.class)).thenReturn(mock(ListenerService.class));
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(ActivityCommonService.class)).thenReturn(mock(ActivityCommonService.class));
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(DocumentService.class)).thenReturn(mock(DocumentService.class));
    wcmCoreUtils.when(() -> WCMCoreUtils.getService(RepositoryService.class)).thenReturn(mock(RepositoryService.class));
    handler = new FileUploadHandler();
  }

  @After
  public void tearDown() {
    wcmCoreUtils.close();
  }

  @Test
  public void abortRemovesTheUploadAsTheRequestUser() throws Exception {
    Response response = handler.control("id1", FileUploadHandler.ABORT_ACTION, request);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(uploadService).removeUploadResource("id1", request);
    verify(uploadService, never()).removeUploadResource(anyString());
  }

  @Test
  public void deleteRemovesTheUploadAsTheRequestUser() throws Exception {
    Response response = handler.control("id1", FileUploadHandler.DELETE_ACTION, request);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
    verify(uploadService).removeUploadResource("id1", request);
    verify(uploadService, never()).removeUploadResource(anyString());
  }

  @Test
  public void aRefusedRemovalAnswers400WithTheReason() throws Exception {
    doThrow(new FileUploadException("Upload id id1 is held by another user")).when(uploadService)
                                                                              .removeUploadResource("id1", request);

    Response response = handler.control("id1", FileUploadHandler.DELETE_ACTION, request);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("Upload id id1 is held by another user", response.getEntity());
    assertTrue(response.getMetadata().getFirst("Cache-Control").toString().contains("no-store"));
  }

  @Test
  public void aRefusedRegistrationAnswers400WithTheReason() throws Exception {
    doThrow(new FileUploadException("Upload id id1 is already registered")).when(uploadService).createUploadResource(request);

    Response response = handler.upload(request, "id1", null);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    assertEquals("Upload id id1 is already registered", response.getEntity());
    assertTrue(response.getMetadata().getFirst("Cache-Control").toString().contains("no-store"));
  }

  /**
   * The limits map refuses a null key since it became concurrent: a progress
   * request naming no upload id answers the empty progress as before
   */
  @Test
  public void aProgressRequestWithoutAnIdAnswersTheEmptyProgress() throws Exception {
    lenient().when(uploadService.getUploadLimits()).thenReturn(new java.util.concurrent.ConcurrentHashMap<>());

    Response response = handler.control(null, FileUploadHandler.PROGRESS_ACTION, request);

    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
  }

  @Test
  public void anUnknownActionAnswers400WithoutRemoving() throws Exception {
    Response response = handler.control("id1", "unknown", request);

    assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    verify(uploadService, never()).removeUploadResource(any(), any());
  }

}
