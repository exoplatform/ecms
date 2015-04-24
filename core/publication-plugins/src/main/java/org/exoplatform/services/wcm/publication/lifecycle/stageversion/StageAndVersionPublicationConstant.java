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
package org.exoplatform.services.wcm.publication.lifecycle.stageversion;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Mar 4, 2009
 */
/**
 * 
 * This class is not supported anymore. So, this class will be removed soon.
 *
 */
public interface StageAndVersionPublicationConstant {

  /** The Constant PUBLICATION_LIFECYCLE_TYPE. */
  public static final String PUBLICATION_LIFECYCLE_TYPE = "publication:stateAndVersionBasedPublication";

  /** The Constant LIFECYCLE_NAME. */
  public static final String LIFECYCLE_NAME             = "States and versions based publication";

  /** The Constant LOCALIZATION. */
  public static final String LOCALIZATION               =
    "locale.services.publication.lifecycle.stageversion.StageAndVersionPublication";

  /** The Constant ENROLLED_TO_LIFECYCLE. */
  public static final String PUBLICATION_LOG_LIFECYCLE  =
    "PublicationService.StageAndVersionPublicationPlugin.changeState.enrolled";

  /** The Constant CHANGE_TO_DRAFT. */
  public static final String PUBLICATION_LOG_DRAFT      =
    "PublicationService.StageAndVersionPublicationPlugin.changeState.draft";

  /** The Constant CHANGE_TO_AWAITNG. */
  public static final String PUBLICATION_LOG_AWAITNG    =
    "PublicationService.StageAndVersionPublicationPlugin.changeState.awaiting";

  /** The Constant CHANGE_TO_LIVE. */
  public static final String PUBLICATION_LOG_LIVE       =
    "PublicationService.StageAndVersionPublicationPlugin.changeState.published";

  /** The Constant CHANGE_TO_OBSOLETE. */
  public static final String PUBLICATION_LOG_OBSOLETE   =
    "PublicationService.StageAndVersionPublicationPlugin.changeState.obsolete";

  /** The Constant RESTORE_VERSION. */
  public static final String PUBLICATION_LOG_RESTORE_VERSION = 
    "PublicationService.StageAndVersionPublicationPlugin.restoreVersion";

  /** The Constant PUBLICATION_LIFECYCLE_NAME. */
  public static final String PUBLICATION_LIFECYCLE_NAME = "publication:lifecycleName";

  /** The Constant CURRENT_STATE. */
  public static final String CURRENT_STATE              = "publication:currentState";

  /** The Constant MIX_VERSIONABLE. */
  public static final String MIX_VERSIONABLE            = "mix:versionable";

  /** The Constant HISTORY. */
  public static final String HISTORY                    = "publication:history";

  /** The Constant LIVE_REVISION_PROP. */
  public static final String LIVE_REVISION_PROP         = "publication:liveRevision";

  /** The Constant LIVE_DATE_PROP. */
  public static final String LIVE_DATE_PROP             = "publication:liveDate";

  /** The Constant REVISION_DATA_PROP. */
  public static final String REVISION_DATA_PROP         = "publication:revisionData";

  /** The Constant RUNTIME_MODE. */
  public static final String RUNTIME_MODE               = "wcm.runtime.mode";

  /** The Constant CURRENT_REVISION_NAME. */
  public static final String CURRENT_REVISION_NAME      = "Publication.context.currentVersion";
  
  public static final String IS_INITIAL_PHASE           = "Publication.context.isInitialPhase";
  
  public static final String DONT_BROADCAST_EVENT       = "Publication.context.dontBroadcastEvent";

  public static final String POST_INIT_STATE_EVENT      = "PublicationService.event.postInitState";
  
  public static final String POST_CHANGE_STATE_EVENT    = "PublicationService.event.postChangeState";

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
