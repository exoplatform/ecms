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
package org.exoplatform.services.jcr.ext.classify.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ISO8601;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.jcr.ext.classify.NodeClassifyPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;



/*
 * Created by The eXo Platform SAS
 * Author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Apr 9, 2008
 */
/**
 * The Class DateTimeClassifyPlugin.
 */
@Deprecated
public class DateTimeClassifyPlugin extends NodeClassifyPlugin {

  /** The Constant MONTHS. */
  private static final int MONTHS = 12;

  /** The template date time. */
  private String templateDateTime;

  /** The property date time. */
  private String propertyDateTime;

  /** The start date time. */
  private Calendar startDateTime;

  /** The end date time. */
  private Calendar endDateTime;

  /** The increment. */
  private int increment;

  /** The increment type. */
  private char incrementType;

  /** The log. */
  private static Log log = ExoLogger.getLogger(DateTimeClassifyPlugin.class);

  /**
   * Instantiates a new date time classify plugin.
   *
   * @param initParams the init params
   */
  public DateTimeClassifyPlugin(final InitParams initParams) {
    try {
      PropertiesParam propertiesParam = initParams.getPropertiesParam("plugin-params");
      String templDateTimeParam = propertiesParam.getProperty("DateTimeTemplate");
      String startDateTimeParam = propertiesParam.getProperty("StartTime");
      String endDateTimeParam = propertiesParam.getProperty("EndTime");
      propertyDateTime = propertiesParam.getProperty("DateTimePropertyName");
      if ("".equals(propertyDateTime)) propertyDateTime = "exo:dateCreated";
      templateDateTime = getWellTemplDateTime(templDateTimeParam);
      startDateTime = getCalendar(startDateTimeParam);
      endDateTime = getCalendar(endDateTimeParam);
      setIncrement();
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform DateTimeClassifyPlugin: ", e);
      }
    }
  }

  /* (non-Javadoc)
   * @see org.exoplatform.services.jcr.ext.classify.NodeClassifyPlugin#classifyChildrenNode(javax.jcr.Node)
   */
  public void classifyChildrenNode(Node parent) throws Exception {
    Session session = parent.getSession();
    for (NodeIterator nodes = parent.getNodes(); nodes.hasNext();) {
      Node node = nodes.nextNode();
      Calendar calNode = node.getProperty(propertyDateTime).getDate();
      if ((calNode.before(startDateTime)) || (calNode.after(endDateTime))) continue;
      Node currentNode = createNewDateTimeNode(parent, getDateTimeStructured(templateDateTime, calNode));
      String srcPath = node.getPath();
      String destPath = currentNode.getPath() + "/" + node.getName();
      session.move(srcPath, destPath);
      session.save();
    }
  }

  /**
   * Gets the date time structured.
   *
   * @param templDateTime the templ date time
   * @param calendar the calendar
   *
   * @return the date time structured
   */
  private String getDateTimeStructured(String templDateTime, Calendar calendar) {
    int year =  calendar.get(Calendar.YEAR),
    month =  calendar.get(Calendar.MONTH) + 1,
    woy =  calendar.get(Calendar.WEEK_OF_YEAR),
    wom =  calendar.get(Calendar.WEEK_OF_MONTH),
    dom =  calendar.get(Calendar.DAY_OF_MONTH),
    dow =  calendar.get(Calendar.DAY_OF_WEEK),
    startYear = startDateTime.get(Calendar.YEAR),
    startMonth = startDateTime.get(Calendar.MONTH);
    switch (incrementType) {
    case 'Y':
      int n = (year - startYear) / (increment + 1);
      year = n * (increment + 1) + startYear;
      break;
    case 'M':
      if (year != startYear) startMonth = 1;
      int m = (month - startMonth) / (increment + 1);
      month = m * (increment + 1) + startMonth;
      break;
    default: break;
    }
    templDateTime = templDateTime.replace("YYYY", Integer.toString(year))
    .replace("MM", Integer.toString(month))
    .replace("WW", Integer.toString(woy))
    .replace("ww", Integer.toString(wom))
    .replace("DD", Integer.toString(dom))
    .replace("dd", Integer.toString(dow));
    String expr = templDateTime.substring(templDateTime.indexOf("{") + 1 , templDateTime.indexOf("}"));
    templDateTime = templDateTime.replace(expr, operateExpression(expr))
    .replace("#", "").replace("{", "")
    .replace("}", "");
    return templDateTime;
  }


  /**
   * Operate expression.
   *
   * @param expression the expression
   *
   * @return the string
   */
  private String operateExpression(String expression) {
    String [] items = StringUtils.split(expression, "+");
    int rel = Integer.parseInt(items[0]) + Integer.parseInt(items[1]);
    int endTime = endDateTime.get(Calendar.YEAR);
    if ((incrementType == 'Y') && (rel > endTime)) rel = endTime;
    else if ((incrementType == 'M') && (rel > MONTHS)) rel = MONTHS;
    String result  = Integer.toString(rel);
    return result;
  }

  /**
   * Creates the new date time node.
   *
   * @param parentNode the parent node
   * @param path the path
   *
   * @return the node
   *
   * @throws Exception the exception
   */
  private Node createNewDateTimeNode(Node parentNode, String path) throws Exception {
    String [] items = path.split("/");
    Node currNode = null;
    for (int i = 0; i < items.length; i++) {
      try {
        currNode = parentNode.getNode(items[i]);
      } catch (Exception e) { currNode = parentNode.addNode(items[i]); }
      parentNode = currNode;
    }
    return currNode;
  }

  /**
   * Gets the well templ date time.
   *
   * @param templ the templ
   *
   * @return the well templ date time
   */
  private String getWellTemplDateTime(String templ) {
    templ = templ.replace(" ", "");
    List<String> temp = new ArrayList<String>();
    String [] items = templ.split("/");
    for (String item : items) {
      if (item.contains("-")) {
        if (item.contains("#")) {
          String subStr = item.substring(item.indexOf("+") + 1 , item.indexOf("}"));
          try {
            Integer.parseInt(subStr);
            item = item.replace("+", "").replace(subStr, "");
            if ((!"YYYY-#{YYYY}".equals(item)) && (!"MM-#{MM}".equals(item))) {
              templ = "";
              break;
            }
          } catch (Exception e) {
            templ = "";
            break;
          }
        } else {
          String [] subItems = item.split("-");
          for (String subItem : subItems) temp.add(subItem);
        }
      } else if (!isValidField(item)) {
        templ = "";
        break;
      }
    }
    return templ;
  }

  /**
   * Checks if is valid field.
   *
   * @param field the field
   *
   * @return true, if is valid field
   */
  private boolean isValidField(String field) {
    if ((!"YYYY".equals(field)) && (!"MM".equals(field)) && (!"WW".equals(field.toUpperCase()))
        && (!"DD".equals(field.toUpperCase())))
      return false;
    return true;
  }


  /**
   * Gets the calendar.
   *
   * @param datetime the datetime
   *
   * @return the calendar
   */
  private Calendar getCalendar(String datetime) {
    Calendar calendar = new GregorianCalendar();
    try {
      calendar = ISO8601.parse(datetime);
      return calendar;
    } catch (Exception e) {
      if (log.isErrorEnabled()) {
        log.error("Error when perform getCalendar: ", e);
      }
    }
    return null;
  }

  /**
   * Sets the increment.
   */
  private void setIncrement() {
    String subStr = templateDateTime.substring(templateDateTime.indexOf("+") + 1 , templateDateTime.indexOf("}"));
    increment = Integer.parseInt(subStr);
    incrementType = templateDateTime.charAt(templateDateTime.indexOf("{") + 1);
  }

}
