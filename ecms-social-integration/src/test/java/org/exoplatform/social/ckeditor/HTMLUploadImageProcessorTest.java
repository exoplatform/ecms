package org.exoplatform.social.ckeditor;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.Workspace;

import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cms.link.LinkManager;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;

import java.io.File;

@RunWith(MockitoJUnitRunner.class)
public class HTMLUploadImageProcessorTest {

  @Rule
  public TemporaryFolder uploadFolder = new TemporaryFolder();

  @Mock
  private PortalContainer portalContainer;

  @Mock
  private UploadService uploadService;

  @Mock
  private RepositoryService repositoryService;

  @Mock
  private ManageableRepository repository;

  @Mock
  private RepositoryEntry repositoryEntry;

  @Mock
  private Session session;

  @Mock
  private LinkManager linkManager;

  @Test
  public void shouldReturnSameContentWhenNoEmbeddedImage() throws Exception {
    // Given
    HTMLUploadImageProcessor imageProcessor = new HTMLUploadImageProcessor(portalContainer, uploadService, repositoryService, linkManager);
    String content = "<p>content with no images</p>";
    Node node = mock(Node.class);

    // When
    String processedContent = imageProcessor.processImages(content, node, null);

    // Then
    assertEquals(content, processedContent);
  }

  @Test
  public void shouldReturnUpdatedContentWhenEmbeddedImage() throws Exception {
    // Given
    HTMLUploadImageProcessor imageProcessor = new HTMLUploadImageProcessor(portalContainer, uploadService, repositoryService, linkManager);
    String content = "<p>content with image: <img src=\"/portal/image?uploadId=123456\" /></p>";
    Node node = mock(Node.class);
    File imageFile = uploadFolder.newFile("image.png");
    UploadResource uploadImage = new UploadResource("123456", "image.png");
    uploadImage.setStoreLocation(imageFile.getPath());
    when(uploadService.getUploadResource(eq("123456"))).thenReturn(uploadImage);
    when(node.hasNode(eq("image.png"))).thenReturn(false);
    when(node.addNode(anyString(), anyString())).thenReturn(node);
    when(portalContainer.getName()).thenReturn("portal");
    when(portalContainer.getRestContextName()).thenReturn("rest");
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repositoryEntry.getName()).thenReturn("repository");
    when(node.getSession()).thenReturn(session);
    Workspace workspace = mock(Workspace.class);
    when(session.getWorkspace()).thenReturn(workspace);
    when(workspace.getName()).thenReturn("collaboration");
    when(node.getPath()).thenReturn("/path/to/image.png");

    // When
    String processedContent = imageProcessor.processImages(content, node, null);

    // Then
    assertEquals("<p>content with image: <img src=\"/portal/rest/jcr/repository/collaboration/path/to/image.png\" /></p>", processedContent);
  }
}