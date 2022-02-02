/*
 * Copyright (C) 2021 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.attachments.plugin.task;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.attachments.plugin.AttachmentACLPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.task.dto.TaskDto;
import org.exoplatform.task.exception.EntityNotFoundException;
import org.exoplatform.task.service.TaskService;
import org.exoplatform.task.util.TaskUtil;

public class TaskAttachmentACLPlugin extends AttachmentACLPlugin {

  private static final Log    LOG                  = ExoLogger.getLogger(TaskAttachmentACLPlugin.class.getName());

  private static final String TASK_ATTACHMENT_TYPE = "task";

  private TaskService         taskService;

  public TaskAttachmentACLPlugin(TaskService taskService) {
    this.taskService = taskService;  }

  @Override
  public String getEntityType() {
    return TASK_ATTACHMENT_TYPE;
  }

  @Override
  public boolean canView(long userIdentityId, String entityType, String entityId) {
    return isProjectParticipant(userIdentityId, entityType, entityId);
  }

  @Override
  public boolean canEdit(long userIdentityId, String entityType, String entityId) {
    return isProjectParticipant(userIdentityId, entityType, entityId);
  }

  @Override
  public boolean canDetach(long userIdentityId, String entityType, String entityId) {
    return isProjectParticipant(userIdentityId, entityType, entityId);
  }

  private boolean isProjectParticipant(long userIdentityId, String entityType, String entityId) {
    if (!entityType.equals(TASK_ATTACHMENT_TYPE)) {
      throw new IllegalArgumentException("Entity type must be" + TASK_ATTACHMENT_TYPE);
    }

    if (StringUtils.isEmpty(entityId)) {
      throw new IllegalArgumentException("Entity id must not be Empty");
    }

    if (userIdentityId <= 0) {
      throw new IllegalArgumentException("User identity must be positive");
    }

    boolean isParticipant = false;
    try {
      TaskDto task = taskService.getTask(Long.parseLong(entityId));
      isParticipant = TaskUtil.hasEditPermission(taskService, task);
    } catch (EntityNotFoundException e) {
      LOG.error("Can not find task with ID: " + entityId);
    }

    return isParticipant;
  }
}
