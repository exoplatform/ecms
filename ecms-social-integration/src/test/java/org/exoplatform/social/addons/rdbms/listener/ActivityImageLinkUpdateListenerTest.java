/*
 * Copyright (C) 2017 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.social.addons.rdbms.listener;

import java.net.URL;
import java.text.DecimalFormat;
import java.time.YearMonth;
import java.util.*;

import javax.jcr.*;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.testing.BaseCommonsTestCase;
import org.exoplatform.component.test.*;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.*;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.upload.UploadService;
import org.junit.Assert;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.ROOT, path = "conf/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/portal/configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/ecms-test-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-configuration.xml"),
})
public class ActivityImageLinkUpdateListenerTest extends BaseCommonsTestCase {
  private static final String ATTACHED_FILE_NAME = "fileName.xml";

  private static final Log        LOG         = ExoLogger.getLogger(ActivityImageLinkUpdateListenerTest.class);

  private ActivityManager         activityManager;

  private MockUploadService       uploadService;

  private List<ExoSocialActivity> tearDownActivityList;

  private Identity                rootIdentity;

  private DecimalFormat           monthFormat = new DecimalFormat("00");

  @Override
  protected void beforeRunBare() {
    setForceContainerReload(true);

    // This is used to make a workaround for embedded file path
    // see
    // org.exoplatform.services.cms.mimetype.DMSMimeTypeResolver.DMSMimeTypeResolver()
    System.setProperty("mock.portal.dir", System.getProperty("gatein.test.output.path") + "/test-classes");
    super.beforeRunBare();
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    IdentityManager identityManager = getContainer().getComponentInstanceOfType(IdentityManager.class);
    activityManager = getContainer().getComponentInstanceOfType(ActivityManager.class);
    uploadService = (MockUploadService) getContainer().getComponentInstanceOfType(UploadService.class);
    rootIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "root");
    tearDownActivityList = new ArrayList<>();
    Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
    org.exoplatform.services.security.Identity identity = authenticator.createIdentity("root");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    IdentityRegistry identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    identityRegistry.register(identity);
    ExoContainerContext.setCurrentContainer(getContainer());
    SessionProviderService sessionProviderService = getContainer().getComponentInstanceOfType(SessionProviderService.class);
    sessionProviderService.setSessionProvider(null, new SessionProvider(conversationState));
  }

  @Override
  protected void tearDown() throws Exception {
    for (ExoSocialActivity activity : tearDownActivityList) {
      try {
        activityManager.deleteActivity(activity.getId());
      } catch (Exception e) {
        LOG.warn("can not delete activity with id: " + activity.getId());
      }
    }
    NodeHierarchyCreator nodeHierarchyCreator = getContainer().getComponentInstanceOfType(NodeHierarchyCreator.class);
    Node userNode = nodeHierarchyCreator.getUserNode(SessionProviderService.getSystemSessionProvider(), "root");
    if (userNode != null) {
      removeFile(userNode);
      userNode.save();
    }
    super.tearDown();
  }

  public void testShouldUpdateImagesLinksWhenImageEmbeddedInBody() {
    // Given
    String uploadId = String.valueOf((long) (Math.random() * 100000L));
    String body = "<img src=\"http://localhost:8080/test?uploadId=" + uploadId + "\" />";

    // When
    ExoSocialActivity activity = createActivityWithEmbeddedImage(rootIdentity, body, uploadId, null);

    // Then
    assertTrue(activity.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    activity = activityManager.getActivity(activity.getId());
    assertTrue(activity.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    assertEquals(0, uploadService.getUploadResources().size());
  }

  public void testShouldUpdateImagesLinksWhenImageEmbeddedWithoutDomainInBody() {
    // Given
    String uploadId = String.valueOf((long) (Math.random() * 100000L));
    String body = "<img src=\"/test?uploadId=" + uploadId + "\" />";

    // Then
    ExoSocialActivity activity = createActivityWithEmbeddedImage(rootIdentity, body, uploadId, null);

    // When
    assertTrue(activity.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    activity = activityManager.getActivity(activity.getId());
    assertTrue(activity.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    assertEquals(0, uploadService.getUploadResources().size());
  }

  public void testShouldUpdateImagesLinksWhenImageEmbeddedWithoutDomainInBodyAndFilesWithSameName() {
    // Given
    String uploadId1 = String.valueOf((long) (Math.random() * 100000L));
    String body1 = "<img src=\"http://localhost:8080/test?uploadId=" + uploadId1 + "\" />";
    String uploadId2 = String.valueOf((long) (Math.random() * 100000L));
    String body2 = "<img src=\"http://localhost:8080/test?uploadId=" + uploadId2 + "\" />";

    // When
    ExoSocialActivity activity1 = createActivityWithEmbeddedImage(rootIdentity, body1, uploadId1, null);
    ExoSocialActivity activity2 = createActivityWithEmbeddedImage(rootIdentity, body2, uploadId2, null);

    // Then
    assertTrue(activity1.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    activity1 = activityManager.getActivity(activity1.getId());
    assertTrue(activity1.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    assertTrue(activity2.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    activity2 = activityManager.getActivity(activity2.getId());
    assertTrue(activity2.getBody().matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    assertEquals(0, uploadService.getUploadResources().size());
  }

  public void testShouldUpdateImagesLinksWhenImageEmbeddedInTemplateParam() {
    // Given
    String uploadId = String.valueOf((long) (Math.random() * 100000L));
    String body = "body";
    Map<String, String> templateParams = new HashMap<>();
    templateParams.put("comment", "<img src=\"http://localhost:8080/test?uploadId=" + uploadId + "\" />");

    // When
    ExoSocialActivity activity = createActivityWithEmbeddedImage(rootIdentity, body, uploadId, templateParams);

    // Then
    assertTrue(activity.getTemplateParams().get("comment")
            .matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    activity = activityManager.getActivity(activity.getId());
    assertTrue(activity.getTemplateParams().get("comment")
            .matches("<img src=\"/portal/rest/images/repository/collaboration/[a-z0-9]+\" />"));
    assertEquals(0, uploadService.getUploadResources().size());
  }

  /**
   * Creates activity on user's stream.
   * 
   * @param user
   * @return
   */
  private ExoSocialActivity createActivityWithEmbeddedImage(Identity user,
                                                            String body,
                                                            String uploadId,
                                                            Map<String, String> templateParams) {
    URL resource = getClass().getResource("/test.xml");

    try {
      uploadService.createUploadResource(uploadId, resource.getPath(), ATTACHED_FILE_NAME, "text/xml");
    } catch (Exception e) {
      fail("Error uploading file", e);
    }

    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setTitle("title");
    activity.setBody(body);
    activity.setPosterId(user.getId());
    activity.setUserId(user.getId());
    activity.setTemplateParams(templateParams);
    try {
      activityManager.saveActivityNoReturn(user, activity);
      tearDownActivityList.add(activity);
    } catch (Exception e) {
      fail("Error creating activity", e);
    } finally {
      end();
      begin();
    }
    return activity;
  }

  private void removeFile(Node userNode) throws RepositoryException {
    LOG.info("+++ Start removing file from path: {}", userNode.getPath());

    NodeIterator nodesIterator = userNode.getNodes();
    while (nodesIterator.hasNext()) {
      Node node = nodesIterator.nextNode();
      if (StringUtils.equals(ATTACHED_FILE_NAME, node.getName())) {
        LOG.info("Removing file from path: {}", node.getPath());
        node.remove();
      } else if (node.hasNodes()) {
        removeFile(node);
      }
    }
  }

}
