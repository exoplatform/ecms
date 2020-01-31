/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wcm.ext.component.activity;

import org.exoplatform.social.webui.activity.BaseUIActivity;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Mar
 * 15, 2011
 */
@ComponentConfig(lifecycle = UIFormLifecycle.class, template = "war:/groovy/ecm/social-integration/UISharedContent.gtmpl", events = {
    @EventConfig(listeners = ContentUIActivity.ViewDocumentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LoadLikesActionListener.class),
    @EventConfig(listeners = BaseUIActivity.ToggleDisplayCommentFormActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LikeActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.SetCommentListStatusActionListener.class),
    @EventConfig(listeners = BaseUIActivity.PostCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.DeleteCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.LikeCommentActionListener.class),
    @EventConfig(listeners = BaseUIActivity.EditActivityActionListener.class),
    @EventConfig(listeners = BaseUIActivity.EditCommentActionListener.class)})
public class SharedContentUIActivity extends ContentUIActivity {



  public static final String ACTIVITY_TYPE      = "CONTENT_ACTIVITY";

  public static final String ID                 = "id";

  public static final String CONTENT_LINK       = "contenLink";

  public static final String MESSAGE            = "message";

  public static final String REPOSITORY         = "repository";

  public static final String WORKSPACE          = "workspace";

  public static final String CONTENT_NAME       = "contentName";

  public static final String IMAGE_PATH         = "imagePath";

  public static final String MIME_TYPE          = "mimeType";

  public static final String STATE              = "state";

  public static final String AUTHOR             = "author";

  public static final String DATE_CREATED       = "dateCreated";

  public static final String LAST_MODIFIED      = "lastModified";

  public static final String DOCUMENT_TYPE_LABEL= "docTypeLabel";
  
  public static final String DOCUMENT_TITLE     = "docTitle";
  
  public static final String DOCUMENT_VERSION   = "docVersion";
  
  public static final String DOCUMENT_SUMMARY   = "docSummary";

  public static final String IS_SYSTEM_COMMENT  = "isSystemComment";
  
  public static final String SYSTEM_COMMENT     = "systemComment";
  
  public static final String MIX_VERSION       = "mix:versionable";

  public SharedContentUIActivity() throws Exception {
    super();
  }
}
