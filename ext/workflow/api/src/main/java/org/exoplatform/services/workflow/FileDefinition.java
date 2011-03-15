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
package org.exoplatform.services.workflow;

import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Base interface of all classes that map a deployable Business Process Archive.
 * It was created to facilitate the support of other archive types in the
 * Bonita Service implementation. For instance, it is possible to create an
 * archive that stores the process definition as a single executable java class.
 *
 * Created by Bull R&D
 * @author Brice Revenant
 * Feb 21, 2005
 */
public interface FileDefinition {
  /**
   * Deploys the process contained by this file definition
   *
   * @throws Exception if a problem occured
   */
  public void deploy() throws Exception;

  /**
   * Retrieves the Customized View name of a Form corresponding to a State name
   *
   * @param  stateName identifies the Form
   * @return a String giving the Customized View name
   */
  public String getCustomizedView(String stateName);

  /**
   * Retrieves all entries as a hastable
   * @return hashtable: (K,V) = file name, byte[]
   */
  public Hashtable<String, byte[]> getEntries();

  /**
   * Retrieve the contents of an entry contained by this file definition
   *
   * @param path location of the entry in the file definition
   * @return the contents of the specified entry
   * @throws Exception if the specified entry is not found
   */
  public byte[] getEntry(String path) throws Exception;

  /**
   * Retrieves the name of the Process model defined by the XPDL definition
   *
   * @return name of the defined Process model
   */
  public String getProcessModelName();

  /**
   * Retrieves the Resource Bundle corresponding to a Form and a Locale.
   * If no Resource Bundle is defined for that Locale, then the default one is
   * retrieved.
   *
   * @param  stateName identifies the Form
   * @return ResourceBundle corresponding to the specified Form and Locale
   */
  public ResourceBundle getResourceBundle(String stateName, Locale locale);

  /**
   * Retrieves the variables definition of a Form corresponding to a State name.
   *
   * @param  stateName identifies the Form
   * @return a <tt>List</tt> of <tt>Map</tt> containing each variable attributes
   */
  public List<Map<String, Object>> getVariables(String stateName);

  /**
   * Determines if the Form corresponding to a State name is a delegated view
   *
   * @param  stateName identifies the Form
   * @return a boolean indicating if the view is delegated
   */
  public boolean isDelegatedView(String stateName);

  /**
   * Indicates if a Form is explicitly defined in forms.xml
   *
   * @param  stateName identifies the Form
   * @return true if the Form is explicitly defined
   */
  public boolean isFormDefined(String stateName);
}
