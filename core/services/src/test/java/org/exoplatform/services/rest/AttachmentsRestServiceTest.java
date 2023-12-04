package org.exoplatform.services.rest;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.download.DownloadService;
import org.exoplatform.services.attachments.model.Attachment;
import org.exoplatform.services.attachments.service.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.ws.rs.core.Response;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentsRestServiceTest {

  @Mock
  private AttachmentService attachmentService;

  private AttachmentsRestService attachmentsRestService;

  @Mock
  private IdentityManager identityManager;
  @Mock
  private DownloadService downloadService;

  @Mock
  private Attachment attachment;

  @Before
  public void setUp() throws Exception {
    when(attachmentService.getAttachmentById(anyString())).thenReturn(attachment);
    attachmentsRestService = new AttachmentsRestService(attachmentService, identityManager, downloadService);
  }

  @Test
  public void getAttachmentById() throws Exception {
    String attachmentId = "1";
    Response response = attachmentsRestService.getAttachmentById(null);
    assertEquals(400, response.getStatus());

    response = attachmentsRestService.getAttachmentById(attachmentId);
    assertEquals(200, response.getStatus());

    when(attachmentService.getAttachmentById(anyString())).thenReturn(null);
    response = attachmentsRestService.getAttachmentById(attachmentId);
    assertEquals(404, response.getStatus());

    when(attachmentService.getAttachmentById(anyString())).thenThrow(new ObjectNotFoundException("attachment is missing"));
    response = attachmentsRestService.getAttachmentById(attachmentId);
    assertEquals(404, response.getStatus());
  }
}
