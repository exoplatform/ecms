/*
 * Copyright (C) 2010 Egg Prod (by benjamin paillereau)
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.services.wcm.publication;

import org.exoplatform.services.wcm.core.NodeLocation;

import javax.jcr.Node;
import java.util.HashMap;
import java.util.List;

/**
 * Date: 12/05/11
 * 
 * @author <a href="mailto:bpaillereau@gmail.com">Benjamin Paillereau</a>
 */
public class Result {

  private NodeLocation nodeLocationDescriber;

  private HashMap<String, String> filtersDescriber;

  private List<Node> nodes;

  private long offset;

  private long numTotal;

  public Result(List<Node> nodes, long offset, long numTotal,
      NodeLocation nodeLocationDescriber,
      HashMap<String, String> filtersDescriber) {
    this.nodes = nodes;
    this.offset = offset;
    this.numTotal = numTotal;
    this.filtersDescriber = filtersDescriber;
    this.nodeLocationDescriber = nodeLocationDescriber;
  }

  public NodeLocation getNodeLocationDescriber() {
    return nodeLocationDescriber;
  }

  public HashMap<String, String> getFiltersDescriber() {
    return filtersDescriber;
  }

  public List<Node> getNodes() {
    return nodes;
  }

  public long getOffset() {
    return offset;
  }

  public long getNumTotal() {
    return numTotal;
  }
}
