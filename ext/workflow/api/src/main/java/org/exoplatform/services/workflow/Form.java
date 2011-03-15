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

import java.util.List;
import java.util.ResourceBundle;

/**
 * Created y the eXo platform team
 * User: Benjamin Mestrallet
 * Date: 17 mai 2004
 */
public interface Form {

  public List getVariables();

  public List getSubmitButtons();

  public String getStateName();


  public ResourceBundle getResourceBundle();

  public boolean isCustomizedView();

  public String getCustomizedView();

  public boolean isDelegatedView();

  public String getIconURL();

  public String getStateImageURL();

}
