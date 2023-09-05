package org.exoplatform.wcm.ext.component.activity.listener;

import static org.exoplatform.services.jcr.ext.ActivityTypeUtils.EXO_ACTIVITY_INFO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Property;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;

import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.space.model.Space;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

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
@RunWith(MockitoJUnitRunner.class)
public class UtilsTest {

  @Mock
  ActivityManager activityManager;

  @Mock
  IdentityManager identityManager;

  @Mock
  ActivityCommonService activityCommonService;

  @Mock
  SpaceService spaceService;

  MockedStatic<CommonsUtils> COMMONS_UTILS;

  MockedStatic<Utils> UTILS;

  @Before
  public void setUp() throws Exception {
    COMMONS_UTILS = mockStatic(CommonsUtils.class);
    UTILS = mockStatic(Utils.class);
    COMMONS_UTILS.when(() -> activityManager.isActivityTypeEnabled(nullable(String.class))).thenReturn(true);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(eq(IdentityManager.class))).thenReturn(identityManager);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(eq(ActivityCommonService.class))).thenReturn(activityCommonService);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(eq(SpaceService.class))).thenReturn(spaceService);
    when(spaceService.getSpaceByGroupId(nullable(String.class))).thenReturn(new Space());

    UTILS.when(() -> Utils.postFileActivity(any(), nullable(String.class), anyBoolean(), anyBoolean(), nullable(String.class), nullable(String.class))).thenCallRealMethod();

    UTILS.when(() -> Utils.getActivityOwnerId(any()))
            .thenReturn("john");
    UTILS.when(() -> Utils.isPublic(any())).thenCallRealMethod();
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    activity.setId("123");
    UTILS.when(() -> Utils.createActivity(any(), nullable(String.class), any(), nullable(String.class), nullable(String.class), anyBoolean(), nullable(String.class), nullable(String.class)))
            .thenReturn(activity);
    when(activityManager.getActivity(anyString())).thenReturn(activity);
    when(identityManager.getOrCreateIdentity(eq(OrganizationIdentityProvider.NAME), any())).thenReturn(new Identity());
  }

  @After
  public void tearDown() throws Exception {
    COMMONS_UTILS.close();
    UTILS.close();
  }

  @Test
  public void shouldPostFileActivityWhenFileIsPublic() throws Exception {
    // Given

    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isCheckedOut()).thenReturn(true);
    when(node.isNodeType(EXO_ACTIVITY_INFO)).thenReturn(false);
    when(node.canAddMixin(EXO_ACTIVITY_INFO)).thenReturn(true);
    lenient().when(node.hasProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    when(node.getACL()).thenReturn(acl);

    // When
    Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, times(1)).saveActivityNoReturn(nullable(Identity.class), any(ExoSocialActivity.class));
  }

  @Test
  public void shouldNotPostFileActivityWhenFileIsNotPublic() throws Exception {
    ExtendedNode node = mock(ExtendedNode.class);
    lenient().when(node.isCheckedOut()).thenReturn(true);
    when(node.isNodeType(EXO_ACTIVITY_INFO)).thenReturn(false);
    lenient().when(node.canAddMixin(EXO_ACTIVITY_INFO)).thenReturn(true);
    lenient().when(node.hasProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(false);    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("*:/spaces/space1", "read")));
    when(node.getACL()).thenReturn(acl);

    // When
    ExoSocialActivity activity = Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, never()).saveActivityNoReturn(any(), any(ExoSocialActivity.class));
  }

  @Test
  public void checkPostActivityIfActivityTypeIsEnabled() throws Exception {
    ExtendedNode node = mock(ExtendedNode.class);
    when(node.isCheckedOut()).thenReturn(true);
    when(node.isNodeType(EXO_ACTIVITY_INFO)).thenReturn(false);
    when(node.canAddMixin(EXO_ACTIVITY_INFO)).thenReturn(true);
    lenient().when(node.hasProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(false);
    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    when(node.getACL()).thenReturn(acl);

    //Enable activity type
    ActivityManager activityManager = mock(ActivityManager.class);
    when(activityManager.isActivityTypeEnabled(nullable(String.class))).thenReturn(true);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);

    // Check File activity when it is disabled
    // When
    Utils.postFileActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, times(1)).saveActivityNoReturn(nullable(Identity.class), any(ExoSocialActivity.class));

    // Check Content activity when it is disabled
    // When
    lenient().when(activityManager.isActivityTypeEnabled(nullable(String.class))).thenReturn(true);
    Utils.postActivity(node, null, false, false, "", "");

    // Then
    verify(activityManager, times(1)).saveActivityNoReturn(nullable(Identity.class), any(ExoSocialActivity.class));
  }
  @Test
  public void checkPostActivityIfActivityTypeIsDisabled() throws Exception {
    ExtendedNode node = mock(ExtendedNode.class);
    lenient().when(node.isCheckedOut()).thenReturn(true);
    lenient().when(node.isNodeType(EXO_ACTIVITY_INFO)).thenReturn(false);
    lenient().when(node.canAddMixin(EXO_ACTIVITY_INFO)).thenReturn(true);
    lenient().when(node.hasProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(false);

    AccessControlList acl = new AccessControlList("john", Arrays.asList(new AccessControlEntry("any", "read")));
    lenient().when(node.getACL()).thenReturn(acl);

    //Disable activity type
    ActivityManager activityManager = mock(ActivityManager.class);
    when(activityManager.isActivityTypeEnabled(nullable(String.class))).thenReturn(false);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(eq(ActivityManager.class))).thenReturn(activityManager);

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

  @Test
  public void testAddVersionComment() {
    UTILS.when(() -> Utils.addVersionComment(any(Node.class), anyString(), anyString())).thenCallRealMethod();
    UTILS.when(() -> Utils.getSpaceName(any())).thenReturn("/spaces/spaceOne");
    ConversationState conversionState = ConversationState.getCurrent();
    if(conversionState == null) {
      conversionState = new ConversationState(new org.exoplatform.services.security.Identity("root"));
      ConversationState.setCurrent(conversionState);
    }

    Node node = mock(Node.class);
    String commentText = "this is a comment";
    String userName = "root";
    try {
      when(node.isCheckedOut()).thenReturn(true);
      when(node.isNodeType(EXO_ACTIVITY_INFO)).thenReturn(false);
      when(node.canAddMixin(EXO_ACTIVITY_INFO)).thenReturn(true);
      lenient().when(node.hasProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(false);
      Utils.addVersionComment(node, commentText, userName);
      verify(activityManager,times(1)).saveComment(any(), any());
    } catch (Exception e) {
      fail();
    }
    try {
      when(node.isNodeType(EXO_ACTIVITY_INFO)).thenReturn(true);
      Property property = mock(Property.class);
      when(property.getString()).thenReturn("444");
      when(node.getProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(property);
      when(activityManager.getActivity(eq("444"))).thenReturn(null);
      lenient().when(node.hasProperty(ActivityTypeUtils.EXO_ACTIVITY_ID)).thenReturn(true);
      Utils.addVersionComment(node, commentText, userName);
      verify(activityManager,times(2)).saveComment(any(), any());
    } catch (Exception e) {
      fail();
    }
  }

}
