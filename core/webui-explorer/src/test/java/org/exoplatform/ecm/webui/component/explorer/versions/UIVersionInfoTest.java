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

    public void testRemoveVersionNode() throws Exception{

        Node file = session.getRootNode().addNode("testXLSFile", "nt:file");
        Node contentNode = file.addNode("jcr:content", "nt:resource");
        contentNode.setProperty("jcr:encoding", "UTF-8");
        contentNode.setProperty("jcr:data", new ByteArrayInputStream("".getBytes()));
        contentNode.setProperty("jcr:mimeType", "application/excel");
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());

        session.getRootNode().save();
        createNodeHistory(file);

        VersionNode versionNode = new VersionNode(file, session);
        //root Version
        assertEquals("2", versionNode.getDisplayName());
        List<VersionNode> versionNodes= versionNode.getChildren();
        assertEquals(2, versionNodes.size());

        Comparator<VersionNode> comparator = new Comparator<VersionNode>() {

            @Override
            public int compare(VersionNode o1, VersionNode o2) {
                return o1.getName().compareTo(o2.getName());
            }
        };

        versionNodes.sort(comparator);

        assertEquals("1", versionNodes.get(0).getName());
        assertEquals("0", versionNodes.get(0).getDisplayName());

        //base Version
        assertEquals("2", versionNodes.get(1).getName());
        assertEquals("1", versionNodes.get(1).getDisplayName());

        //remove Base Version
        VersionHistoryUtils.removeVersion(file,"2");
        versionNode = new VersionNode(file, session);

        versionNodes.clear();
        //root Version
        assertEquals("3", versionNode.getDisplayName());
        versionNodes = versionNode.getChildren();

        assertEquals(1, versionNodes.size());

        versionNodes.sort(comparator);

        assertEquals("1", versionNodes.get(0).getName());
        assertEquals("0", versionNodes.get(0).getDisplayName());

        //add new Version
        VersionHistoryUtils.createVersion(file);
        versionNode = new VersionNode(file, session);
        versionNodes.clear();

        //root Version
        assertEquals("4", versionNode.getDisplayName());
        versionNodes = versionNode.getChildren();

        assertEquals(2, versionNodes.size());

        versionNodes.sort(comparator);

        assertEquals("1", versionNodes.get(0).getName());
        assertEquals("0", versionNodes.get(0).getDisplayName());

        //base Version : nt:version name 2; display name 3
        assertEquals("2", versionNodes.get(1).getName());
        assertEquals("3", versionNodes.get(1).getDisplayName());

    }

    private void createNodeHistory(Node node) throws  RepositoryException {
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
        //V1
        node.checkin();
        node.checkout();

        //V2
        node.checkin();
        node.checkout();
    }
}
