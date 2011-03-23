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


package hero.hook;

import org.ow2.bonita.definition.TxHook;
import org.ow2.bonita.facade.APIAccessor;
import org.ow2.bonita.facade.runtime.ActivityBody;
import org.ow2.bonita.facade.runtime.ActivityInstance;


/**
 * This Node Hook gets the user name who started the Instance and puts it
 * in "initiator" attribute. It is used to print the initiator name into a
 * Form component. The Hook also puts the "choice" attribute to "enough".
 * It is used to force the Workflow to go to the "evaluation" activity.
 *
 * Created by Bull R&D
 * @author Rodrigue Le Gall
 */
public class HolidayEnoughHolidaysLeftHook implements TxHook {

  /**
   * Name of the Property that contains the decision if the workflow initiator
   * have enough holidays left or not
   */
  public static final String CHOICE_PROPERTY_NAME = "choice";

  /**
   * Name of the Property that contains the workflow initiator login
   */
  public static final String PROCESS_INITIATOR_USER_NAME = "initiator";


  public void execute(APIAccessor accessor, ActivityInstance<ActivityBody> activity) throws Exception {
    // Retrieve workflow session creator and set the instance property
    // "initiator" with it
    String user_Name = accessor.getQueryRuntimeAPI()
                               .getProcessInstance(activity.getProcessInstanceUUID())
                               .getStartedBy();
    accessor.getRuntimeAPI().setProcessInstanceVariable(activity.getProcessInstanceUUID(),
                                                        PROCESS_INITIATOR_USER_NAME,
                                                        user_Name);
    accessor.getRuntimeAPI().setActivityInstanceVariable(activity.getUUID(),
                                                         CHOICE_PROPERTY_NAME,
                                                         "enough");

  }
}
