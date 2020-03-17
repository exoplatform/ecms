package org.exoplatform.wcm.ext.component.activity.listener;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.jcr.Node;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
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
import org.exoplatform.services.jcr.ext.ActivityTypeUtils;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;

/**
 * Test class for org.exoplatform.wcm.ext.component.activity.listener.Utils
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ Utils.class, CommonsUtils.class })
public class UtilsTest {
  @Test
  public void shouldPostFileActivityWhenFileIsPublic() throws Exception {
    // Given
    ActivityManager activityManager = mock(ActivityManager.class);
    IdentityManager identityManager = mock(IdentityManager.class);
    ActivityCommonService activityCommonService = mock(ActivityCommonService.class);
    SpaceService spaceService = mock(SpaceService.class);
    PowerMockito.mockStatic(CommonsUtils.class);
    when(activityManager.isActivityTypeEnabled(anyString())).thenReturn(true);
    when(CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);
    when(CommonsUtils.getService(eq(IdentityManager.class))).thenReturn(identityManager);
    when(CommonsUtils.getService(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    when(CommonsUtils.getService(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(null);

    PowerMockito.mockStatic(Utils.class);
    when(Utils.postFileActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();

    Utils spy = PowerMockito.spy(new Utils());
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "getActivityOwnerId", Node.class))
                .withArguments(any())
                .thenReturn("john");
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "isPublic", Node.class)).withArguments(any()).thenCallRealMethod();
    PowerMockito.when(spy,
                      MemberMatcher.method(Utils.class,
                                           "createActivity",
                                           IdentityManager.class,
                                           String.class,
                                           Node.class,
                                           String.class,
                                           String.class,
                                           boolean.class,
                                           String.class,
                                           String.class))
                .withArguments(any(), anyString(), any(), anyString(), anyString(), anyBoolean(), anyString(), anyString())
                .thenReturn(new ExoSocialActivityImpl());

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    when(node.getACL()).thenReturn(acl);

    // When
    Utils.postFileActivity(node, null, false, false, "", "");

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
    PowerMockito.mockStatic(CommonsUtils.class);
    when(CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);
    when(activityManager.isActivityTypeEnabled(anyString())).thenReturn(true);
    when(CommonsUtils.getService(eq(IdentityManager.class))).thenReturn(identityManager);
    when(CommonsUtils.getService(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    when(CommonsUtils.getService(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(null);

    PowerMockito.mockStatic(Utils.class);
    when(Utils.postFileActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();

    Utils spy = PowerMockito.spy(new Utils());
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "getActivityOwnerId", Node.class))
                .withArguments(any())
                .thenReturn("john");
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "isPublic", Node.class)).withArguments(any()).thenCallRealMethod();

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("*:/spaces/space1", "read")));
    when(node.getACL()).thenReturn(acl);

    // When
    ExoSocialActivity activity = Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, never()).saveActivityNoReturn(any(), any(ExoSocialActivity.class));
  }

  @Test
  public void checkPostActivityIfActivityTypeIsEnabled() throws Exception {
    // Given
    IdentityManager identityManager = mock(IdentityManager.class);
    ActivityCommonService activityCommonService = mock(ActivityCommonService.class);
    SpaceService spaceService = mock(SpaceService.class);
    PowerMockito.mockStatic(CommonsUtils.class);
    when(CommonsUtils.getService(eq(IdentityManager.class))).thenReturn(identityManager);
    when(CommonsUtils.getService(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    when(CommonsUtils.getService(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(null);

    PowerMockito.mockStatic(Utils.class);
    when(Utils.postFileActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();
    when(Utils.postActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();

    Utils spy = PowerMockito.spy(new Utils());
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "getActivityOwnerId", Node.class))
                .withArguments(any())
                .thenReturn("john");
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "isPublic", Node.class)).withArguments(any()).thenCallRealMethod();
    PowerMockito.when(spy,
                      MemberMatcher.method(Utils.class,
                                           "createActivity",
                                           IdentityManager.class,
                                           String.class,
                                           Node.class,
                                           String.class,
                                           String.class,
                                           boolean.class,
                                           String.class,
                                           String.class))
                .withArguments(any(), anyString(), any(), anyString(), anyString(), anyBoolean(), anyString(), anyString())
                .thenReturn(new ExoSocialActivityImpl());

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    when(node.getACL()).thenReturn(acl);

    //Enable activity type
    ActivityManager activityManager = mock(ActivityManager.class);
    when(activityManager.isActivityTypeEnabled(anyString())).thenReturn(true);
    when(CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);

    // Check File activity when it is disabled
    // When
    Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, times(1)).saveActivityNoReturn(any(), any(ExoSocialActivity.class));

    // Check Content activity when it is disabled
    // When
    reset(activityManager);
    when(activityManager.isActivityTypeEnabled(anyString())).thenReturn(true);
    Utils.postActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, times(1)).saveActivityNoReturn(any(), any(ExoSocialActivity.class));
  }
  @Test
  public void checkPostActivityIfActivityTypeIsDisabled() throws Exception {
    // Given
    IdentityManager identityManager = mock(IdentityManager.class);
    ActivityCommonService activityCommonService = mock(ActivityCommonService.class);
    SpaceService spaceService = mock(SpaceService.class);
    PowerMockito.mockStatic(CommonsUtils.class);
    when(CommonsUtils.getService(eq(IdentityManager.class))).thenReturn(identityManager);
    when(CommonsUtils.getService(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    when(CommonsUtils.getService(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(anyString())).thenReturn(null);

    PowerMockito.mockStatic(Utils.class);
    when(Utils.postFileActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();
    when(Utils.postActivity(any(), anyString(), anyBoolean(), anyBoolean(), anyString(), anyString())).thenCallRealMethod();

    Utils spy = PowerMockito.spy(new Utils());
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "getActivityOwnerId", Node.class))
            .withArguments(any())
            .thenReturn("john");
    PowerMockito.when(spy, MemberMatcher.method(Utils.class, "isPublic", Node.class)).withArguments(any()).thenCallRealMethod();
    PowerMockito.when(spy,
            MemberMatcher.method(Utils.class,
                    "createActivity",
                    IdentityManager.class,
                    String.class,
                    Node.class,
                    String.class,
                    String.class,
                    boolean.class,
                    String.class,
                    String.class))
            .withArguments(any(), anyString(), any(), anyString(), anyString(), anyBoolean(), anyString(), anyString())
            .thenReturn(new ExoSocialActivityImpl());

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isNodeType(ActivityTypeUtils.EXO_ACTIVITY_INFO)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    when(node.getACL()).thenReturn(acl);

    //Disable activity type
    ActivityManager activityManager = mock(ActivityManager.class);
    when(activityManager.isActivityTypeEnabled(anyString())).thenReturn(false);
    when(CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);

    // Check File activity when it is disabled
    // When

    Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, never()).saveActivityNoReturn(any(), any(ExoSocialActivity.class));


    // Check Content activity when it is disabled
    // When
    Utils.postActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, never()).saveActivityNoReturn(any(), any(ExoSocialActivity.class));
  }

}
