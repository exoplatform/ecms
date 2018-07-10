package org.exoplatform.ecm.webui.component.explorer.versions;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.ecm.jcr.model.VersionNode;
import org.exoplatform.ecms.test.BaseECMSTestCase;
import org.exoplatform.services.cms.documents.VersionHistoryUtils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.wcm.core.NodetypeConstant;


import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;


@ConfiguredBy({
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/mock-rest-configuration.xml"),
        @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/ecms/configuration.xml")
})
public class UIVersionInfoTest extends BaseECMSTestCase {

    public void testVersioning() throws Exception {
        // Given
        Node file = session.getRootNode().addNode("testXLSFile", "nt:file");
        Node contentNode = file.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
        contentNode.setProperty("jcr:mimeType", "application/excel");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.getRootNode().save();

        addNodeVersion(file); // V1
        addNodeVersion(file); // V2

        // When
        VersionNode versionNode = new VersionNode(file, session);

        // Then
        assertEquals("3", versionNode.getName()); //root Version
        assertEquals("2", versionNode.getDisplayName());
        List<VersionNode> versionNodes = versionNode.getChildren();
        assertNotNull(versionNodes);
        assertEquals(2, versionNodes.size());
        versionNodes.sort(Comparator.comparing(VersionNode::getName));
        assertEquals("1", versionNodes.get(0).getName());
        assertEquals("0", versionNodes.get(0).getDisplayName());
        assertEquals("2", versionNodes.get(1).getName());
        assertEquals("1", versionNodes.get(1).getDisplayName());
    }

    public void testRemoveVersionNode() throws Exception {
        // Given
        Node file = session.getRootNode().addNode("testXLSFile", "nt:file");
        Node contentNode = file.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
        contentNode.setProperty("jcr:mimeType", "application/excel");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.getRootNode().save();

        addNodeVersion(file); // V1
        addNodeVersion(file); // V2

        // When
        VersionHistoryUtils.removeVersion(file,"1");

        // Then
        VersionNode versionNode = new VersionNode(file, session);
        assertEquals("3", versionNode.getName());
        assertEquals("2", versionNode.getDisplayName());

        List<VersionNode> versionNodes = versionNode.getChildren();
        assertEquals(1, versionNodes.size());

        versionNodes.sort(Comparator.comparing(VersionNode::getName));
        assertEquals("2", versionNodes.get(0).getName());
        assertEquals("1", versionNodes.get(0).getDisplayName());
    }

    public void testRemoveAndCreateVersionNode() throws Exception{
        // Given
        Node file = session.getRootNode().addNode("testXLSFile", "nt:file");
        Node contentNode = file.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
        contentNode.setProperty("jcr:mimeType", "application/excel");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        session.getRootNode().save();

        addNodeVersion(file); // V1
        addNodeVersion(file); // V2

        // When
        VersionHistoryUtils.removeVersion(file,"2");
        VersionHistoryUtils.createVersion(file);

        // Then
        VersionNode versionNode = new VersionNode(file, session);
        assertEquals("3", versionNode.getName());
        assertEquals("3", versionNode.getDisplayName());

        List<VersionNode> versionNodes = versionNode.getChildren();
        assertEquals(2, versionNodes.size());

        versionNodes.sort(Comparator.comparing(VersionNode::getName));
        assertEquals("1", versionNodes.get(0).getName());
        assertEquals("0", versionNodes.get(0).getDisplayName());
        assertEquals("2", versionNodes.get(1).getName());
        assertEquals("2", versionNodes.get(1).getDisplayName());
    }

    private void addNodeVersion(Node node) throws  RepositoryException {
        if(node.canAddMixin(NodetypeConstant.MIX_VERSIONABLE)){
            node.addMixin(NodetypeConstant.MIX_VERSIONABLE);
            node.save();
        }
        ConversationState conversationState = ConversationState.getCurrent();
        String userName = (conversationState == null) ? node.getSession().getUserID() :
                conversationState.getIdentity().getUserId();
        if(node.canAddMixin("exo:modify")) {
            node.addMixin("exo:modify");
        }
        node.setProperty("exo:lastModifiedDate", new GregorianCalendar());
        node.setProperty("exo:lastModifier",userName);
        node.save();

        // add version
        node.checkin();
        node.checkout();
    }
}
