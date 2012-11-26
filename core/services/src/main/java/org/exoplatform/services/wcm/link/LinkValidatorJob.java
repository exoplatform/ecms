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
package org.exoplatform.services.wcm.link;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.scheduler.BaseJob;
import org.exoplatform.services.scheduler.JobContext;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong_phan@exoplatform.com
 * Aug 4, 2008
 */
public class LinkValidatorJob extends BaseJob {

  private static final Log LOG = ExoLogger.getLogger(LinkValidatorJob.class.getName());

  public void execute(JobContext arg0) throws Exception {
    LiveLinkManagerService linkManagerService = WCMCoreUtils.getService(LiveLinkManagerService.class);
    if(linkManagerService == null) return;
    try {
      linkManagerService.updateLinks();
    } catch (Exception e) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Error when execute link validator job by scheduler", e);
      }
    }
  }

}
