/***************************************************************************
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
 *
 **************************************************************************/
package org.exoplatform.services.cms.actions;

import javax.jcr.observation.Event;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Jan 4, 2010
 */

/**
 * Extends javax.jcr.observation.Event for defining more event Now, Event has 4
 * defined event types: NODE_ADDED, NODE_REMOVED, PROPERTY_ADDED, PROPERTY_REMOVED,
 * PROPERTY_CHANGED. DMS added more event below to support some events come from dms
 * and get value of event type by name
 */
public abstract class DMSEvent implements Event {

  public static final int READ          = 2048;

  public static final int SCHEDULE      = 4096;

  public static int getEventTypes(String[] lifecycle) throws NoSuchFieldException, IllegalAccessException {
    if (lifecycle == null)
      throw new IllegalArgumentException("lifecycle null");
    int eventType = 0;
    for (String event : lifecycle) {
      eventType = eventType | getEventType(event);
    }
    return eventType;
  }

  public static int getEventType(String event) throws NoSuchFieldException, IllegalAccessException {
    if (event == null) throw new IllegalArgumentException("event null");
    try {
      return DMSEvent.class.getField(event.toUpperCase()).getInt(null);
    } catch (NoSuchFieldException ns){
      throw ns;
    } catch (IllegalAccessException ace) {
      throw ace;
    }
  }
}
