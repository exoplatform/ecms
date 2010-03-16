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
package org.exoplatform.services.workflow.test;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.exoplatform.services.workflow.Form;
import org.exoplatform.services.workflow.impl.jbpm.WorkflowServiceContainerImpl;
import org.jbpm.db.JbpmSession;
import org.jbpm.graph.def.ProcessDefinition;


/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 17 mai 2004
 */
public class TestForm extends BaseTest{

  private static final String PROCESS_FILE = "processdefinition.xml";

  public TestForm(String name) {
    super(name);
  }

  protected String getDescription() {
    return "test workflow forms";
  }

  public void setUp() {
    super.setUp();
    String[] files = {"forms.xml", "start.properties",
        "evaluation.properties", "hr.properties"};
    try {
      deployProcess(PROCESS_FILE, files);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void testForms() {
    JbpmSession session = ((WorkflowServiceContainerImpl) workflowServiceContainer).openSession();
        
    List definitions = session.getGraphSession().findAllProcessDefinitions();
    ProcessDefinition definition = null;
    for (Iterator iter = definitions.iterator(); iter.hasNext();) {
      ProcessDefinition currentDefinition = (ProcessDefinition) iter.next();
      System.out.println("CUREENNTT DEF " + currentDefinition.getName());
      if("pay raise process".equals(currentDefinition.getName())){
        definition = currentDefinition;
        break;
      }
    }
    session.close();
    
    Form form = workflowFormsService.getForm(new Long(definition.getId()).toString(), "start", new Locale("en"));
    assertNotNull(form);
    assertEquals("start", form.getStateName());
    ResourceBundle rB = form.getResourceBundle();
    assertEquals("Let's pray", rB.getString("submit"));
    assertEquals("the amount you want your salary to be increased",
        rB.getString("amount-asked.title"));

    List map = form.getSubmitButtons();
    assertTrue(map.isEmpty());

    List variables = form.getVariables();
    assertTrue(!variables.isEmpty());
    Map attributes = (Map)variables.get(0);
    assertNotNull(attributes);
  }
}
