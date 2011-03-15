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

import org.exoplatform.services.workflow.Form;

import java.util.Hashtable;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains Forms that were previously created so
 * that they do not need to be created every time.
 *
 * Created by Bull R&D
 * @author Rodrigue Le Gall,Brice Revenant
 * Feb 21, 2006
 */
public class FormCache {

  private static Logger log = Logger.getLogger(FormCache.class.getName());

  /**
   * Contains the Forms currently stored. The indexation is the following one :
   * Process Id -> State Name -> Locale -> Form
   */
  Hashtable<String, Hashtable<String, Hashtable<Locale, Form>>> forms =
    new Hashtable<String, Hashtable<String, Hashtable<Locale, Form>>>();

  /**
   * Retrieves a Form from the cache based on specified information
   *
   * @param processId identifies the process
   * @param activity identifies the state
   * @param locale    locale in which the Form should be retrieved
   * @return the requested Form or null if missing
   */
  public Form getForm(String processId,
                      String activity,
                      Locale locale) {
    // Retrieve the states hashtable based on the Process identifier
    Hashtable<String, Hashtable<Locale, Form>> states = forms.get(processId);
    if(states == null) {
      return null;
    }

    // Retrieve the locales hashtable based on the state name
    Hashtable<Locale, Form> locales = states.get(activity);
    if(locales == null) {
      return null;
    }

    // Retrieve the form based on the Locale
    return locales.get(locale);
  }

  /**
   * Remove all Forms corresponding to a Process Model
   *
   * @param processId identifies the process
   */
  public void removeForms(String processId) {
    // Remove the entry from the Hashtable
    forms.remove(processId);
  }

  /**
   * Puts a Form in the cache based on specified information
   *
   * @param processId identifies the process
   * @param activity identifies the state
   * @param locale    locale of the form
   * @param form      the Form to be cached
   */
  public void setForm(String processId,
                      String activity,
                      Locale locale,
                      Form   form) {
    if (log.isLoggable(Level.INFO)) {
          log.info("Form will be cached for [process,activity]: [" + processId + "," +activity + "]");
        }
    // Retrieve or create the states hashtable
    Hashtable<String, Hashtable<Locale, Form>> states = forms.get(processId);
    if(states == null) {
      states = new Hashtable<String, Hashtable<Locale, Form>>();
      forms.put(processId, states);
    }

    // Retrieve or create the locales hashtable
    Hashtable<Locale, Form> locales = states.get(activity);
    if(locales == null) {
      locales = new Hashtable<Locale, Form>();
      states.put(activity, locales);
    }

    // Put the form in the locales hashtable
    locales.put(locale, form);
  }

  /**
   * Creates a String representation of the cache for debugging purpose
   * @return a String representation of the cache
   */
  public String toString() {
    StringBuffer ret = new StringBuffer();

    for(String processId : forms.keySet()) {
      ret.append("Process Id = " + processId + "\n");
      Hashtable<String, Hashtable<Locale, Form>> states = forms.get(processId);

      for(String activity : states.keySet()) {
        ret.append("  State Name = " + activity + "\n");
        Hashtable<Locale, Form> locales = states.get(activity);

        for(Locale locale : locales.keySet()) {
          ret.append("    Locale = " + locale.toString() + "\n");
        }
      }
    }

    return ret.toString();
  }
}
