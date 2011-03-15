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
package org.exoplatform.services.workflow.impl.bonita;

import java.util.Date;

import org.exoplatform.services.workflow.Timer;

/**
 * Created by Bull R&D
 * @author Brice Revenant
 * Jun 12, 2006
 */
public class TimerData implements Timer {

  private Date dueDate = null;
  private String id    = null;
  private String name  = null;

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Timer#getDueDate()
   */
  public Date getDueDate() {
    return this.dueDate;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Timer#getId()
   */
  public String getId() {
    return this.id;
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.workflow.Timer#getName()
   */
  public String getName() {
    return this.name;
  }

  /**
   * Constructs a new Timer Data instance based on the specified Bonita object
   *
   * @param timerData contains Timer information
   */
//  public TimerData(hero.util.TimerData timerData) {
//
//    // The pattern is "task:process_instance_id:date"
//    String[] type  = timerData.getType().split(":");
//
//    // Set the class attributes
//    this.dueDate = new Date(timerData.getMs());
//    this.id      = type[0];
//    this.name    = type[1];
//  }
}
