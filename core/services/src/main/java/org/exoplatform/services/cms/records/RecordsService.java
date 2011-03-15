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
package org.exoplatform.services.cms.records;

import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

public interface RecordsService {

  /**
   * Add action for filePlan node in repository
   * @param filePlan Node to process
   * @param repository Repository name`
   * @throws Exception
   */
  public void bindFilePlanAction(Node filePlan, String repository) throws Exception;

  /**
   * Set property for filePlan node which is get from record node
   *
   * @param filePlan filePlan Node
   * @param record record Node
   * @throws RepositoryException
   */
  public void addRecord(Node filePlan, Node record) throws RepositoryException;

  /**
   * Determine if the next phase is a hold, transfer or destruction
   * @param filePlan
   * @throws RepositoryException
   */
  public void computeCutoffs(Node filePlan) throws RepositoryException;

  /**
   * Process transfer or destruction
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public void computeHolds(Node filePlan) throws RepositoryException;

  /**
   * Copy record node in filePlan node to path which value is
   * rma:transferLocation property of filePlan Node
   * @param filePlan
   * @throws RepositoryException
   */
  public void computeTransfers(Node filePlan) throws RepositoryException;

  /**
   * Copy record node in filePlan node to path which value is
   * rma:accessionLocation property of filePlan Node
   * @param filePlan
   * @throws RepositoryException
   */
  public void computeAccessions(Node filePlan) throws RepositoryException;

  /**
   * Remove record node in filePlan node
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public void computeDestructions(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:vitalRecord
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getVitalRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:isObsolete
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getObsoleteRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:superseded
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getSupersededRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:cutoffExecuted
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getCutoffRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:holdExecuted
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getHolableRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:transferExecuted
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getTransferableRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of node by query statement with constraint concerning @rma:accessionExecuted
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getAccessionableRecords(Node filePlan) throws RepositoryException;

  /**
   * Get list of rma:destroyable node by query statement @param filePlan filePlan node
   * @param filePlan filePlan node
   * @throws RepositoryException
   */
  public List<Node> getDestroyableRecords(Node filePlan) throws RepositoryException;
}
