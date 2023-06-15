package org.exoplatform.services.attachments.listener;

import org.exoplatform.services.attachments.service.AttachmentService;
import org.exoplatform.services.listener.Event;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AttachmentViewListenerTest {

  @Mock
  private AttachmentService      attachmentService;

  private AttachmentViewListener attachmentViewListener;

  @Before
  public void setUp() throws Exception {
    attachmentViewListener = new AttachmentViewListener(attachmentService);
  }

  @Test
  public void onEvent() throws Exception {
    attachmentViewListener.onEvent(new Event("update-document-views-detail", "user", "123"));
    verify(attachmentService, times(1)).markAttachmentAsViewed("123", "user");
  }
}
