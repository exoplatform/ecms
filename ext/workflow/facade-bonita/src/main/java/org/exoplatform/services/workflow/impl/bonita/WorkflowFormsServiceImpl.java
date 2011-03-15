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

import java.util.Locale;

import org.exoplatform.services.workflow.FileDefinition;
import org.exoplatform.services.workflow.Form;
import org.exoplatform.services.workflow.WorkflowFileDefinitionService;
import org.exoplatform.services.workflow.WorkflowFormsService;

/**
 * This service retrieves and caches Forms
 *
 * Created by Bull R&D
 * @author Rodrigue Le Gall
 * @author Brice Revenant
 * Dec 28, 2005
 */
public class WorkflowFormsServiceImpl implements WorkflowFormsService {

  /** Caches the Forms that have been created so far */
  private FormCache cache = new FormCache();

  /** Reference to a File Definition Service implementation */
  private WorkflowFileDefinitionService fileDefinitionService = null;

  /**
   * Retrieves a Form based on a process model, a state and a Locale. As the
   * process instance identifier is not specified as parameter, the attributes
   * shown are only those defined in the process model, which means propagable
   * attributes are ignored.
   * @param processId identifies the process
   * @param stateName identifies the activity
   * @param locale    specifies the Locale
   */
  public Form getForm(String processId,
                      String stateName,
                      Locale locale) {

    // Determine if the Form is cached yet
    Form form = cache.getForm(processId, stateName, locale);

    if(form == null) {
      // The Form is not found. Retrieve it from the persistent storage
      FileDefinition fileDefinition =
        this.fileDefinitionService.retrieve(processId);

      if(fileDefinition != null && fileDefinition.isFormDefined(stateName)) {
        // The Form is found in the storage and defined
        form = new SpecifiedFormImpl(processId,fileDefinition, stateName, locale);
      } else {
        // The Form is not found in the storage and not defined
        form = new AutomaticFormImpl(processId, stateName, locale);
      }

      // Cache the Form to speed up subsequent accesses
      cache.setForm(processId, stateName, locale, form);
    }

    return form;
  }

  /**
   * Remove all Forms corresponding to a Process Model
   *
   * @param processDefinitionId identifies the Process Model
   */
  public void removeForms(String processDefinitionId) {

    // Remove the specified Forms from the cache
    this.cache.removeForms(processDefinitionId);
  }

  /**
   * Creates a new instance of the service
   *
   * @param fileDefinitionService this injected reference to a File Definition
   *                              service is used to store and retrieved
   *                              definitions of processes, which include
   *                              among others Forms.
   */
  public WorkflowFormsServiceImpl(
      WorkflowFileDefinitionService fileDefinitionService) {
    // Store references to dependent services
    this.fileDefinitionService = fileDefinitionService;
  }
}
