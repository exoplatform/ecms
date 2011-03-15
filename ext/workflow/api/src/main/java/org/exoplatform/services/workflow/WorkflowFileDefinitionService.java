/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.workflow;

/**
 * This interface specifies the contract of the Service which manages File
 * Definitions. A File Definition contains among other things Forms definitions.
 * It is required to persist File definitions somewhere as Forms need to be
 * retrieved if eXo is restarted. It was decided to define a Service to do that,
 * which makes it possible to allow various types of storage (eg: File System,
 * ECM).
 *
 * <i>This interface is currently part of the Bonita
 * package as jBPM has a
 * built-in facility to manage File Definition. It may however be a good idea to
 * move it to the api
 * package to make things common.</i>
 *
 * Created by Bull R&D
 * @author Brice Revenant
 * Feb 27, 2005
 */
public interface WorkflowFileDefinitionService {

  /**
   * Remove a File Definition
   *
   * @param processId identifies the File Definition to remove
   */
  public void remove(String processId);

  /**
   * If the implementation features a cache to increase performances, removes
   * the File Definition corresponding to the specified Process identifier.
   * This method is notably used while reloading a File Definition.
   *
   * @param processId identifies the Process to be removed from the cache
   */
  public void removeFromCache(String processId);

  /**
   * Retrieves a File Definition
   *
   * @param  processId identifies the File Definition to retrieve
   * @return the requested File Definition or <tt>null</tt> if not found
   */
  public FileDefinition retrieve(String processId);

  /**
   * Stores a File Definition
   *
   * @param fileDefinition the File Definition to store
   * @param processId      identifies the File Definition to store
   */
  public void store(FileDefinition fileDefinition, String processId);
}
