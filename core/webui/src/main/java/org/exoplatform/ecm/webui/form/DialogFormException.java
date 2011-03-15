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
package org.exoplatform.ecm.webui.form;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.web.application.ApplicationMessage;

/**
 * Created by eXo Platform
 * Author : Nguyen Manh Cuong
 *          manhcuongpt@gmail.com
 * Jun 26, 2009
 */

/**
 * This is an exception which will be thrown when there is a problem with pre save interceptor
 */
public class DialogFormException extends Exception {

  private List<ApplicationMessage> appMsgList = new ArrayList<ApplicationMessage>();

  public DialogFormException(ApplicationMessage app){
    appMsgList.add(app);
  }

  public void addApplicationMessage(ApplicationMessage app) {
    appMsgList.add(app);
  }

  public List<ApplicationMessage> getMessages() {
    return appMsgList;
  }
}
