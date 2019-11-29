package org.exoplatform.wcm.ext.component.activity.listener;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.jcr.Node;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.support.membermodification.MemberMatcher;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.cms.jcrext.activity.ActivityCommonService;
import org.exoplatform.services.jcr.access.AccessControlEntry;
import org.exoplatform.services.jcr.access.AccessControlList;
import org.exoplatform.services.jcr.core.ExtendedNode;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.utils.ActivityTypeUtils;

/**
 * Test class for org.exoplatform.wcm.ext.component.activity.listener.Utils
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(Utils.class)
public class UtilsTest {
  @Test
  public void shouldPostFileActivityWhenFileIsPublic() throws Exception {
    // Given
    ActivityManager activityManager = mock(ActivityManager.class);
    IdentityManager identityManager = mock(IdentityManager.class);
    ActivityCommonService activityCommonService = mock(ActivityCommonService.class);
    SpaceService spaceService = mock(SpaceService.class);
    ExoContainer exoContainer = mock(ExoContainer.class);
    ExoContainerContext.setCurrentContainer(exoContainer);
    when(exoContainer.getComponentInstanceOfType(eq(ActivityManager.class))).thenReturn(activityManager);
    when(exoContainer.getComponentInstanceOfType(eq(IdentityManager.class))).thenReturn(identityManager);
    when(exoContainer.getComponentInstanceOfType(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    when(exoContainer.getComponentInstanceOfType(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(null);

    PowerMockito.mockStatic(Utils.class);
    when(Utils.postFileActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();

    Utils spy = PowerMockito.spy(new Utils());
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "getActivityOwnerId", Node.class))
            .withArguments(any())
            .thenReturn("john");
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "isPublic", Node.class))
            .withArguments(any())
            .thenCallRealMethod();

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    when(node.getACL()).thenReturn(acl);

    // When
    ExoSocialActivity activity = Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, times(1)).saveActivityNoReturn(any(), any(ExoSocialActivity.class));
  }

  @Test
  public void shouldNotPostFileActivityWhenFileIsNotPublic() throws Exception {
    // Given
    ActivityManager activityManager = mock(ActivityManager.class);
    IdentityManager identityManager = mock(IdentityManager.class);
    ActivityCommonService activityCommonService = mock(ActivityCommonService.class);
    SpaceService spaceService = mock(SpaceService.class);
    ExoContainer exoContainer = mock(ExoContainer.class);
    ExoContainerContext.setCurrentContainer(exoContainer);
    when(exoContainer.getComponentInstanceOfType(eq(ActivityManager.class))).thenReturn(activityManager);
    when(exoContainer.getComponentInstanceOfType(eq(IdentityManager.class))).thenReturn(identityManager);
    when(exoContainer.getComponentInstanceOfType(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    when(exoContainer.getComponentInstanceOfType(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(null);

    PowerMockito.mockStatic(Utils.class);
    when(Utils.postFileActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();

    Utils spy = PowerMockito.spy(new Utils());
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "getActivityOwnerId", Node.class))
            .withArguments(any())
            .thenReturn("john");
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "isPublic", Node.class))
            .withArguments(any())
            .thenCallRealMethod();

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("*:/spaces/space1", "read")));
    when(node.getACL()).thenReturn(acl);

    // When
    ExoSocialActivity activity = Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, never()).saveActivityNoReturn(any(), any(ExoSocialActivity.class));
  }
}