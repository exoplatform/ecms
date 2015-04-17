/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.cms.clipboard;

import java.util.Set;

import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jan 28, 2014  
 */
public interface ClipboardService {
  
  /**
   * Adds a clipboard command for user
   * @param userId user to add command
   * @param command the command 
   * @param isVirtual if the command is virtual
   */
  public void addClipboardCommand(String userId, ClipboardCommand command, boolean isVirtual);
  
  /**
   * Gets the last clipboard command of user
   * @param userId user to get command
   * @param isVirtual if the command is virtual
   * @return the ClipboardCommand
   */
  public ClipboardCommand getLastClipboard(String userId);

  /**
   * Gets the list of clipboard command added by given user
   * check in clipboard. if a node was deleted, remove all clipboardCommands relate to that node in clipboard 
   * @param userId the user who added the commands
   * @param isVirtual if the commands are virtual
   * @return the list of ClipboardCommand
   */
  public Set<ClipboardCommand> getClipboardList(String userId, boolean isVirtual);
  
  /**
   * Clears the list of clipboard command
   * @param userId the user who added the commands
   * @param isVirtual if the commands are virtual
   */
  public void clearClipboardList(String userId, boolean isVirtual);
  
  /**
   * Remove one command form clipboard
   * @param userId the user who added the commands
   * @param isVirtual if the commands are virtual
   * command need to remove
   */
  public void removeClipboardCommand(String userId, ClipboardCommand command);
}
