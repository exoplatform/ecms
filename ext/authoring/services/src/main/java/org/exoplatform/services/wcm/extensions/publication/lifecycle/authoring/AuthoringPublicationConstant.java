/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.services.wcm.extensions.publication.lifecycle.authoring;

/**
 * Created by The eXo Platform SAS Author : Hoa Pham hoa.phamvu@exoplatform.com
 * Mar 4, 2009
 */
public interface AuthoringPublicationConstant {

  /** The Constant PUBLICATION_LIFECYCLE_TYPE. */
  public static final String  PUBLICATION_LIFECYCLE_TYPE = "publication:authoringPublication".intern();

  /** The Constant LIFECYCLE_NAME. */
  public static final String  LIFECYCLE_NAME             = "Authoring publication".intern();

  /** The Constant LOCALIZATION. */
  public static final String  LOCALIZATION               = "artifacts.lifecycle.stageversion.StageAndVersionPublication".intern();

  /** The Constant ENROLLED_TO_LIFECYCLE. */
  public static final String  ENROLLED_TO_LIFECYCLE      = "Publication.log.description.enrolled".intern();

  /** The Constant CHANGE_TO_DRAFT. */
  public static final String  CHANGE_TO_DRAFT            = "PublicationService.AuthoringPublicationPlugin.changeState.draft".intern();

  /** The Constant CHANGE_TO_LIVE. */
  public static final String  CHANGE_TO_LIVE             = "PublicationService.AuthoringPublicationPlugin.changeState.published".intern();

  /** The Constant CHANGE_TO_PENDING. */
  public static final String  CHANGE_TO_PENDING          = "PublicationService.AuthoringPublicationPlugin.changeState.pending".intern();

  /** The Constant CHANGE_TO_APPROVED. */
  public static final String  CHANGE_TO_APPROVED         = "PublicationService.AuthoringPublicationPlugin.changeState.approved".intern();

  /** The Constant CHANGE_TO_OBSOLETE. */
  public static final String  CHANGE_TO_OBSOLETED        = "PublicationService.AuthoringPublicationPlugin.changeState.obsoleted".intern();
  
  /** The Constant CHANGE_TO_STAGED. */
  public static final String  CHANGE_TO_STAGED           = "PublicationService.AuthoringPublicationPlugin.changeState.staged".intern();

  /** The Constant CHANGE_TO_UNPUBLISHED. */
  public static final String  CHANGE_TO_UNPUBLISHED      = "PublicationService.AuthoringPublicationPlugin.changeState.unpublished".intern();

  /** The Constant CHANGE_TO_ARCHIVED. */
  public static final String  CHANGE_TO_ARCHIVED         = "PublicationService.AuthoringPublicationPlugin.changeState.archived".intern();

  /** The Constant PUBLICATION_LIFECYCLE_NAME. */
  public static final String  PUBLICATION_LIFECYCLE_NAME = "publication:lifecycleName".intern();

  /** The Constant CURRENT_STATE. */
  public static final String  CURRENT_STATE              = "publication:currentState".intern();

  /** The Constant MIX_VERSIONABLE. */
  public static final String  MIX_VERSIONABLE            = "mix:versionable".intern();

  /** The Constant HISTORY. */
  public static final String  HISTORY                    = "publication:history".intern();

  /** The Constant LIVE_REVISION_PROP. */
  public static final String  LIVE_REVISION_PROP         = "publication:liveRevision".intern();

  /** The Constant LIVE_DATE_PROP. */
  public static final String  LIVE_DATE_PROP             = "publication:liveDate".intern();

  /** The Constant REVISION_DATA_PROP. */
  public static final String  REVISION_DATA_PROP         = "publication:revisionData".intern();

  /** The Constant RUNTIME_MODE. */
  public static final String  RUNTIME_MODE               = "wcm.runtime.mode".intern();

  /** The Constant CURRENT_REVISION_NAME. */
  public static final String  CURRENT_REVISION_NAME      = "Publication.context.currentVersion".intern();

  /** The Constant START_TIME_PROPERTY. */
  public static final String  START_TIME_PROPERTY        = "publication:startPublishedDate".intern();

  /** The Constant START_TIME_PROPERTY. */
  public static final String  END_TIME_PROPERTY          = "publication:endPublishedDate".intern();

  /** The Constant POST_UPDATE_STATE_EVENT. */
  public static final String POST_UPDATE_STATE_EVENT    = "PublicationService.event.postUpdateState".intern();

  /**
   * The Enum SITE_MODE.
   */
  public static enum SITE_MODE {
    /** The LIVE. */
    LIVE,
    /** The EDITING. */
    EDITING
  };
}
