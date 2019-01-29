package org.exoplatform.ecms.upgrade.sanitization;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cms.BasePath;
import org.exoplatform.services.cms.impl.DMSConfiguration;
import org.exoplatform.services.cms.impl.DMSRepositoryConfiguration;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.services.jcr.core.ExtendedSession;
import org.exoplatform.services.jcr.core.ManageableRepository;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.security.IdentityConstants;
import org.junit.Test;

import javax.jcr.PathNotFoundException;
import java.util.Arrays;

import static org.mockito.Mockito.*;

public class ECMSSecureJCRFoldersUpgradePluginTest {

  @Test
  public void testSecureJCRFoldersMigration() throws Exception {
    // Given
    OrganizationService orgService = mock(OrganizationService.class);

    DMSRepositoryConfiguration config = new DMSRepositoryConfiguration();
    config.setSystemWorkspace("system");
    DMSConfiguration dmsConfiguration = mock(DMSConfiguration.class);
    when(dmsConfiguration.getConfig()).thenReturn(config);

    NodeHierarchyCreator nodeHierarchyCreator = mock(NodeHierarchyCreator.class);
    RepositoryEntry entry = mock(RepositoryEntry.class);
    when(entry.getDefaultWorkspaceName()).thenReturn("collaboration");

    ExtendedSession session = mock(ExtendedSession.class);
    ManageableRepository repo = mock(ManageableRepository.class);
    when(repo.getConfiguration()).thenReturn(entry);
    when(repo.getSystemSession(any())).thenReturn(session);

    RepositoryService repositoryService = mock(RepositoryService.class);
    when(repositoryService.getCurrentRepository()).thenReturn(repo);

    //
    ExtendedNode ecmNode = mock(ExtendedNode.class);
    when(session.getItem("/exo:ecm")).thenReturn(ecmNode);
    String[] relPaths = {"exo:taxonomyTrees/definition", "exo:folksonomies/exo:tagStyles",
        "templates", "scripts", "metadata", "queries", "scripts/ecm-explorer",
        "scripts/ecm-explorer/action", "scripts/ecm-explorer/interceptor", "scripts/ecm-explorer/widget",
        "views", "views/templates", "views/userviews", "views/templates/ecm-explorer"};
    ExtendedNode subEcmNode = mock(ExtendedNode.class);
    for (String path: relPaths) {
      when(ecmNode.getNode(path)).thenReturn(subEcmNode);
    }
    when(ecmNode.getNode("exo:taxonomyTrees/storage")).thenReturn(subEcmNode);

    //
    ExtendedNode groupsNode = mock(ExtendedNode.class);
    when(nodeHierarchyCreator.getJcrPath(BasePath.CMS_GROUPS_PATH)).thenReturn("/Groups");
    when(session.getItem("/Groups")).thenReturn(groupsNode);

    GroupHandler groupHandler = mock(GroupHandler.class);
    when(orgService.getGroupHandler()).thenReturn(groupHandler);
    when(groupHandler.findGroups(null)).thenReturn(Arrays.asList(new GroupImpl("platform"), new GroupImpl("organization"), new GroupImpl("management")));

    ExtendedNode groupNode = mock(ExtendedNode.class);
    when(groupsNode.getNode("platform")).thenReturn(groupNode);
    when(groupsNode.getNode("organization")).thenReturn(groupNode);
    when(groupsNode.getNode("management")).thenThrow(PathNotFoundException.class);
    when(groupNode.getNode("ApplicationData/Tags")).thenReturn(groupNode);
    when(groupNode.getNode("SharedData")).thenReturn(groupNode);

    //
    ExtendedNode digitalNode = mock(ExtendedNode.class);
    String[] EXO_DIGITAL_PATH_ALIAS = new String[]{
        "digitalVideoPath", "digitalAudioPath", "digitalAssetsPath", "digitalPicturePath"
    };
    for (String path: EXO_DIGITAL_PATH_ALIAS) {
      when(nodeHierarchyCreator.getJcrPath(path)).thenReturn("/digital assets");
      when(session.getItem("/digital assets")).thenReturn(digitalNode);
    }

    // When
    ECMSSecureJCRFoldersUpgradePlugin plugin = new ECMSSecureJCRFoldersUpgradePlugin(orgService, repositoryService, dmsConfiguration, nodeHierarchyCreator, new InitParams());
    plugin.processUpgrade("5.1.0", "5.2.0");

    // Then
    verify(ecmNode, times(1)).removePermission(IdentityConstants.ANY);
    verify(subEcmNode, times(14)).removePermission(IdentityConstants.ANY);
    verify(subEcmNode, times(1)).removePermission("*:/platform/users");

    verify(groupsNode, times(1)).removePermission("*:/platform/users");
    verify(groupNode, times(4)).removePermission(IdentityConstants.ANY);

    verify(digitalNode, times(4)).removePermission("*:/platform/users");
  }
}
