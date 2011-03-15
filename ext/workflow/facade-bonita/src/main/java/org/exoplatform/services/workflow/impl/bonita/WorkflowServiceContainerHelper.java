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

import org.ow2.bonita.facade.uuid.ProcessDefinitionUUID;
import org.ow2.bonita.facade.uuid.UUIDFactory;
import org.ow2.bonita.util.AccessorUtil;


/**
 * Contains helper methods for the Bonita Workflow Service implementation
 *
 * Created by Bull R&D
 * @author Brice Revenant
 * Mar 2, 2006
 */
public class WorkflowServiceContainerHelper {

  public static final String getProcessName(String processId) throws Exception{
    ProcessDefinitionUUID uuid = UUIDFactory.getProcessDefinitionUUID(processId);
    return AccessorUtil.getQueryAPIAccessor().getQueryDefinitionAPI().getProcess(uuid).getName();
  }

  /**
   * Retrieves the Process Model name from a Process Instance name.
   * In the past there was a convenient method in BnProject that enabled to
   * retrieve the Model. But it was removed from Bonita for performance
   * reasons. As a consequence, it was decided to retrieve the Project Model
   * in this dedicated helper method.
   *
   * @param  instanceName name of the instance whose model is to be retrieved
   * @return a String corresponding to the Process Model name
   */
  public static final String getModelName(String instanceName) {

//    ProjectSessionLocal projectSession = null;
//    String modelName = null;
//
//    try {
//      // Initialize Project Session
//      ProjectSessionLocalHome projectSessionHome =
//        ProjectSessionUtil.getLocalHome();
//      projectSession = projectSessionHome.create();
//      projectSession.initProject(instanceName);
//
//      // Retrieve the Model name
//      modelName = projectSession.getProjectNameOfInstance(instanceName);
//    }
//    catch(Exception e) {
//      e.printStackTrace();
//    }
//    finally {
//      try {
//        projectSession.remove();
//      }
//      catch(Exception ignore) {
//      }
//    }
//
//    return modelName;
    return "";
  }

  /**
   * Converts a String to an Object.
   * Bonita handles Strings by default. This class provides conversion utility
   * methods to make it possible to deal with Objects in eXo. It uses constants
   * defined in {@link org.exoplatform.portlets.workflow.component.UITask UITask}.
   * However this class is currently bundled in a portlet and not in a jar file
   * which prevents from importing it in Eclipse. As a consequence, those
   * constants cannot be referenced and their value is used instead.
   *
   * @param value the String to be converted
   * @param type  identifies the component in which the value is displayed.
   *              It indicates the type of the object to produce. For
   *              instance, UITask.DATE indicates that the returned object
   *              should be of type Date. It should be one of the constants
   *              defined in
   *              {@link org.exoplatform.portlets.workflow.component.UITask
   *              UITask}.
   * @return      an <tt>Object</tt> representing the specified value
   */
  public static final Object stringToObject(String value, String component) {

    Object ret = null;

    if("date".equals(component) || "datetime".equals(component)) {
      if(value == null || "".equals(value)) {
        // The value is unset, use the current date
        ret = new Date();
      }
      else {
        // The value is assumed to be a long
        ret = new Date(Long.parseLong(value));
      }
    }
    else {
      if(value == null) {
        // Increase robustness by creating an empty String
        ret = "";
      }
      else {
        // By default the returned Object is the specified String
        ret = value;
      }
    }

    return ret;
  }

  /**
   * Converts an Object to a String.
   * Bonita handles Strings by default. This class provides conversion utility
   * methods to make it possible to deal with Objects in eXo. It uses constants
   * defined in {@link org.exoplatform.portlets.workflow.component.UITask UITask}.
   * However this class is currently bundled in a portlet and not in a jar file
   * which prevents from importing it in Eclipse. As a consequence, those
   * constants cannot be referenced and their value is used instead.
   *
   * @param value the Object to be converted
   * @param type  identifies the component in which the value is displayed.
   *              It indicates the expected type of the specified value. For
   *              instance, UITask.DATE indicates that the value is an Object
   *              of type Date. It should be one of the constants defined in
   *              {@link org.exoplatform.portlets.workflow.component.UITask
   *              UITask}.
   * @return      a <tt>String</tt> representing the specified value
   */
  public static final String objectToString(Object value, String component) {

    String ret = null;

    if(value == null) {
      // Increase robustness
      ret = "";
    }
    else if("date".equals(component) || "datetime".equals(component)) {
      /*
       * The value is assumed to be a Date. Convert it to a long that represents
       * the number of milliseconds since January 1, 1970. That way human
       * readable representations are not used, which avoids Localization
       * issues.
       */
      ret = new Long(((Date) value).getTime()).toString();
    }
    else {
      // Delegate to the default Java conversion method
      ret = value.toString();
    }

    return ret;
  }
}
