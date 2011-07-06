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
package org.exoplatform.services.wcm.utils;

import java.util.ArrayList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.exoplatform.services.jcr.impl.core.query.lucene.TwoWayRangeIterator;
import org.exoplatform.services.wcm.core.NodeLocation;

/**
 * Created by The eXo Platform SAS
 * Author : Hoa Pham
 * hoa.phamvu@exoplatform.com
 * Oct 17, 2008
 * This class will be removed in the next release
 */
@Deprecated
public class PaginatedQueryResult extends PaginatedNodeIterator {

  /** The row iterator. */
  private RowIterator rowIterator;

  /**
   * Instantiates a new paginated query result.
   *
   * @param pageSize the page size
   */
  public PaginatedQueryResult(int pageSize) {
    super(pageSize);
  }

  /**
   * Instantiates a new paginated query result.
   *
   * @param queryResult the query result
   * @param pageSize the page size
   *
   * @throws Exception the exception
   */
  @SuppressWarnings("deprecation")
  public PaginatedQueryResult(QueryResult queryResult,int pageSize) throws Exception{
    super(pageSize);
    this.nodeIterator = queryResult.getNodes();
    this.setAvailablePage((int)nodeIterator.getSize());
    this.rowIterator = queryResult.getRows();
  }

  /* (non-Javadoc)
   * @see org.exoplatform.wcm.webui.paginator.PaginatedNodeIterator#populateCurrentPage(int)
   */
  @SuppressWarnings({ "unchecked", "deprecation" })
  protected void populateCurrentPage(int page) throws Exception {
    checkAndSetPosition(page);
    TwoWayRangeIterator twoWayRangeIterator = (TwoWayRangeIterator)nodeIterator;
    currentListPage_ = new ArrayList();
    int count = 0;
    while (nodeIterator.hasNext()) {
      Node node = nodeIterator.nextNode();
      Node viewNode = filterNodeToDisplay(node);
      if(viewNode != null) {
        //Skip back 1 position to get current row mapping to the node
        twoWayRangeIterator.skipBack(1);
        Row row = rowIterator.nextRow();
        ResultNode resultNode = new ResultNode(viewNode,row);
        currentListPage_.add(resultNode);
        count ++;
        if(count == getPageSize())
          break;
      }
    }
    currentPage_ = page;
  }

  /**
   * Filter node to display.
   *
   * @param node the node
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  protected Node filterNodeToDisplay(Node node) throws Exception {
    Node displayNode = node;
    if(node.isNodeType("nt:resource")) {
      displayNode = node.getParent();
    }
    return displayNode;
  }

  /**
   * The Class ResultNode.
   */
  public static class ResultNode {

    /** The node location. */
    private NodeLocation node;

    /** The score. */
    private float score;

    /** The excerpt. */
    private String excerpt;

    /** The spell suggestion. */
    private String spellSuggestion;

    /**
     * Instantiates a new result node.
     *
     * @param node the node
     * @param row the row
     *
     * @throws RepositoryException the repository exception
     */
    public ResultNode(Node node, Row row) throws RepositoryException{
      this.node = NodeLocation.getNodeLocationByNode(node);
      Value excerpt = row.getValue("rep:excerpt(.)");
      this.excerpt = excerpt == null ? "" : excerpt.getString();
      this.spellSuggestion = row.getValue("rep:spellcheck()").getString();
      this.score = row.getValue("jcr:score").getLong();
    }

    /**
     * Gets the node.
     *
     * @return the node
     */
    public Node getNode() { return NodeLocation.getNodeByLocation(node); }

    /**
     * Sets the node.
     *
     * @param node the new node
     */
    public void setNode(Node node) { 
      this.node = NodeLocation.getNodeLocationByNode(node); }

    /**
     * Gets the score.
     *
     * @return the score
     */
    public float getScore() { return score; }

    /**
     * Sets the score.
     *
     * @param score the new score
     */
    public void setScore(float score) { this.score = score; }

    /**
     * Gets the excerpt.
     *
     * @return the excerpt
     */
    public String getExcerpt() {
      return excerpt;
    }

    /**
     * Sets the excerpt.
     *
     * @param excerpt the new excerpt
     */
    public void setExcerpt(String excerpt) {
      this.excerpt = excerpt;
    }

    /**
     * Gets the spell suggestion.
     *
     * @return the spell suggestion
     */
    public String getSpellSuggestion() {
      return spellSuggestion;
    }

    /**
     * Sets the spell suggestion.
     *
     * @param spellSuggestion the new spell suggestion
     */
    public void setSpellSuggestion(String spellSuggestion) {
      this.spellSuggestion = spellSuggestion;
    }
  }
}
