package org.exoplatform.ecms.activity.processor;

import org.exoplatform.services.wcm.core.NodeLocation;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(NodeLocation.class)
@PowerMockIgnore({ "javax.management.*", "javax.xml.*", "org.apache.xerces.*", "org.xml.*" })
public class ActivityAttachmentProcessorTest {

    @Mock
    private ActivityAttachmentProcessor activityAttachmentProcessor;

    @Test
    public void processActivity() {
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        activity.setType("sharefiles:spaces");
        HashMap<String, String> templateParams = new HashMap<>();
        templateParams.put("workspace", "collaboration");
        templateParams.put("author", "root");
        templateParams.put("permission", "read");
        templateParams.put("nodePath", "/Groups/spaces/new/Documents/Shared/sample.pdf");
        templateParams.put("docTitle", "sample.pdf");
        templateParams.put("mimeType", "application/pdf");
        templateParams.put("message", "");
        templateParams.put("repository", "repository");
        templateParams.put("contentName", "sample.pdf");
        templateParams.put("nodeUUID", "736ec0d07f0001015e91bad4c1d582e1");
        templateParams.put("id", "736ec0d07f0001015e91bad4c1d582e1");
        activity.setTemplateParams(templateParams);
        NodeLocation nodeLocation = new NodeLocation(templateParams.get("repository"), templateParams.get("workspace"),
                templateParams.get("nodePath"), templateParams.get("nodeUUID"), true);
        Node currentNode = mock(Node.class);
        doCallRealMethod().when(activityAttachmentProcessor).processActivity(activity);
        PowerMockito.mockStatic(NodeLocation.class);
        when(NodeLocation.getNodeByLocation(ArgumentMatchers.refEq(nodeLocation))).thenReturn(currentNode);
        activityAttachmentProcessor.processActivity(activity);
        assertEquals(1, activity.getFiles().size());
    }
}
