package org.exoplatform.wcm.ext.component.activity;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;

public class UISharedFileBuilderTest {
    private static final Log LOG = ExoLogger.getLogger(UISharedFileBuilderTest.class);

    @Test
    public void testExtendUIActivity() throws Exception {
        String activityTitle = "Test";
        UISharedFileBuilder uiSharedFileBuilder = new UISharedFileBuilder();
        FileUIActivity fileUIActivity = new FileUIActivity();
        ExoSocialActivity activity = new ExoSocialActivityImpl();
        activity.setTitle(activityTitle);
        try {
            uiSharedFileBuilder.extendUIActivity(fileUIActivity, activity);
        } catch (Exception e) {
            LOG.error("Can not get the repository. ");
        }
        assertEquals(activityTitle, fileUIActivity.getMessage());
        assertEquals(activityTitle, fileUIActivity.getActivityTitle());
    }
}
