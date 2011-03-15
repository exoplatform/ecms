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

import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * When forms are automatically generated, it is also needed to provide a
 * bundle so that eXo Portlets can display information. This class is a bundle
 * that provides convenient methods to handle components to be displayed.
 *
 * Created by Bull R&D
 * @author Brice Revenant
 * @author Le Gall Rodrigue <rodrigue.le-gall@bull.net>
 * Feb 10, 2006
 */
public class AutomaticFormBundle extends ResourceBundle {

  private static Logger log = Logger.getLogger(AutomaticFormBundle.class.getName());
  /** Contains resource bundle information */
  private Hashtable<String, Object> objects;

  /**
   * Adds a variable to the bundle so that it can be displayed in forms
   *
   * @param attributes describes the variable. These are the same attributes as
   *                   those used in eXo, except a new one entitled
   *                   <tt>possible-values</tt> that contains a Collection of
   *                   possible values in case the variable is multivalued.
   */
  public void addVariable(Map<String, Object> attributes) {
    // An entry having a ".label" suffix in the key is required
    String variableName = (String) attributes.get("name");
    this.objects.put(variableName + ".label", variableName);

    // Process possible values in case the variable is of select type
    Collection<String> possibleValues = (Collection<String>)
      attributes.get("possible-values");
    if(possibleValues != null) {
      int count = 0;
      for(String possibleValue : possibleValues) {
        this.objects.put(variableName + ".select-" + count + ".value", possibleValue);
        this.objects.put(variableName + ".select-" + count + ".label" , possibleValue);
        count++;
      }
    }
  }

  /**
   * Adds a button to the bundle so that it can be displayed in forms
   *
   * @param attributes describes the button. These are the same attributes as
   *                   those used in eXo.
   */
  public void addButton(Map<String, Object> attributes) {
    String buttonName = (String) attributes.get("name");
    this.objects.put(buttonName + ".submit", buttonName);
  }

  /**
   * Adds a button to the bundle corresponding to the one shown in start panels.
   * This method has been added as the button requires a specific key.
   */
  public void addDefaultButton() {
    // TODO Internationalize me
    this.objects.put("submit", "submit");
  }

  /**
   * Creates a new Resource Bundle for automatically generated forms
   *
   * @param activity name of the state corresponding to the form
   */
  public AutomaticFormBundle(String activity) {
    this.objects = new Hashtable<String, Object>();
    this.objects.put("task-name", activity);
    this.objects.put("title",     activity);
  }

  @Override
  public Enumeration<String> getKeys() {
    return null;
  }

  @Override
  protected Object handleGetObject(String key) {
    return this.objects.get(key);
  }
}
