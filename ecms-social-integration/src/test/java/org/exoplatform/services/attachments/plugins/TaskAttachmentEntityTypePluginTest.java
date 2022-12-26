package org.exoplatform.services.attachments.plugins;

import junit.framework.TestCase;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.jcr.impl.core.NodeImpl;
import org.exoplatform.services.jcr.impl.core.SessionImpl;
import org.exoplatform.services.jcr.impl.core.WorkspaceImpl;
import org.exoplatform.task.dto.ProjectDto;
import org.exoplatform.task.dto.StatusDto;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.service.ProjectService;
import org.exoplatform.task.service.TaskService;
import org.powermock.api.mockito.PowerMockito;

import javax.jcr.nodetype.NodeType;
import java.util.Arrays;
import java.util.HashSet;

import static org.exoplatform.services.wcm.core.NodetypeConstant.NT_FOLDER;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TaskAttachmentEntityTypePluginTest extends TestCase {

  public void testGetAttachmentOrLinkId() throws Exception {
    long entityId = 1;

    // Mock services
    TaskService taskService = PowerMockito.mock(TaskService.class);
    ProjectService projectService = PowerMockito.mock(ProjectService.class);
    NodeHierarchyCreator nodeHierarchyCreator = PowerMockito.mock(NodeHierarchyCreator.class);
    RepositoryService repositoryService = PowerMockito.mock(RepositoryService.class);
    SessionProviderService sessionProviderService = PowerMockito.mock(SessionProviderService.class);

    // Mock SessionProvider
    SessionProvider sessionProvider = PowerMockito.mock(SessionProvider.class);
    when(sessionProviderService.getSessionProvider(null)).thenReturn(sessionProvider);

    // Mock Repository service and JCR session
    ManageableRepository repository = mock(ManageableRepository.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repository);
    RepositoryEntry repositoryEntry = mock(RepositoryEntry.class);
    when(repository.getConfiguration()).thenReturn(repositoryEntry);
    when(repository.getConfiguration().getDefaultWorkspaceName()).thenReturn("collaboration");
    ExtendedSession extendedSession = mock(ExtendedSession.class);
    when(sessionProvider.getSession(any(), any())).thenReturn(extendedSession);

    // Mock Task
    TaskDto task = new TaskDto();
    task.setId(1);
    ProjectDto project = new ProjectDto();
    project.setId(1);
    StatusDto status = new StatusDto();
    status.setProject(project);
    task.setStatus(status);
    when(taskService.getTask(1)).thenReturn(task);
    when(projectService.getParticipator(anyLong())).thenReturn(new HashSet<>(Arrays.asList("user1",
            "/platform/users", "member:/spaces/space1")));

    // Instantiate the TaskAttachmentEntityTypePlugin
    TaskAttachmentEntityTypePlugin taskAttachmentEntityTypePlugin = new TaskAttachmentEntityTypePlugin(taskService,
                                                                                                       projectService,
                                                                                                       nodeHierarchyCreator,
                                                                                                       sessionProviderService,
                                                                                                       repositoryService);

    // Node does not exist, we return the same attachmentId
    String attachmentId = "123456789Azerty";
    String attachmentName = "testFile.docx";
    assertEquals(attachmentId, taskAttachmentEntityTypePlugin.getAttachmentOrLinkId("task", entityId, attachmentId));

    // Node exist
    NodeImpl node = mock(NodeImpl.class);
    when(node.getIdentifier()).thenReturn(attachmentId);
    when(node.getName()).thenReturn(attachmentName);
    NodeType nodeType = mock(NodeType.class);
    when(nodeType.getName()).thenReturn("nt:file");
    when(node.getPrimaryNodeType()).thenReturn(nodeType);
    when(extendedSession.getNodeByIdentifier(anyString())).thenReturn(node);
    when(extendedSession.itemExists(anyString())).thenReturn(true);

    // Create different nodes
    NodeImpl rootNode = mock(NodeImpl.class);
    NodeImpl taskParentNode = mock(NodeImpl.class);
    NodeImpl taskNode = mock(NodeImpl.class);
    NodeImpl linkNode = mock(NodeImpl.class);
    String linkNodeIdentifier = "link_identifier_123456789Azerty";
    SessionImpl session = mock(SessionImpl.class);
    WorkspaceImpl workspace = mock(WorkspaceImpl.class);
    when(workspace.getName()).thenReturn("collaboration");
    when(session.getWorkspace()).thenReturn(workspace);
    when(node.getSession()).thenReturn(session);
    when(linkNode.getIdentifier()).thenReturn(linkNodeIdentifier);
    when(taskNode.addNode(anyString(), anyString())).thenReturn(linkNode);
    when(taskParentNode.getNode(String.valueOf(anyLong()))).thenReturn(taskNode);
    when(taskParentNode.addNode(String.valueOf(entityId), NT_FOLDER)).thenReturn(taskNode);
    when(rootNode.addNode("task", NT_FOLDER)).thenReturn(taskParentNode);
    when(extendedSession.getItem(anyString())).thenReturn(rootNode);

    // Will return link ID instead of the original attached file
    assertEquals(linkNodeIdentifier, taskAttachmentEntityTypePlugin.getAttachmentOrLinkId("task", 1, attachmentId));
  }
}
