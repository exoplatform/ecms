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
package org.exoplatform.ecm.webui.form.field;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.exoplatform.ecm.webui.form.DialogFormField;
import org.exoplatform.webui.form.UIFormDateTimeInput;
import org.exoplatform.webui.form.UIFormInputBase;

/**
 * Created by The eXo Platform SAS
 * @author : Hoa.Pham
 *          hoa.pham@exoplatform.com
 * Jun 23, 2008
 */
public class UIFormCalendarField extends DialogFormField {

  public UIFormCalendarField(String name, String label, String[] args) {
    super(name, label, args);
  }

  @SuppressWarnings("unchecked")
  public <T extends UIFormInputBase> T createUIFormInput() throws Exception {
    String[] arrDate = null;
    SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss") ;
    Date date = null;
    if(options == null) formatter = new SimpleDateFormat("MM/dd/yyyy") ;
    if(defaultValue != null && defaultValue.length() > 0) {
      try {
        date = formatter.parse(defaultValue) ;
        if(defaultValue.indexOf("/") > -1) arrDate = defaultValue.split("/") ;
        String[] arrDf = formatter.format(date).split("/") ;
        if(Integer.parseInt(arrDate[0]) != Integer.parseInt(arrDf[0])) date = new Date() ;
      } catch(Exception e) {
        date = null;
      }
    }
    UIFormDateTimeInput uiDateTime = null;
    if(isDisplayTime()) {
      uiDateTime = new UIFormDateTimeInput(name, name, date) ;
    } else {
      uiDateTime = new UIFormDateTimeInput(name, name, date, false) ;
    }
    if (date != null) {
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(date);
      uiDateTime.setCalendar(calendar);
    } else {
      uiDateTime.setCalendar(null);
    }
    if(label != null) uiDateTime.setLabel(label);
    return (T)uiDateTime;
  }

  public boolean isVisible() {
    if (visible == null) return true;
    return "true".equalsIgnoreCase(visible);
  }
  public boolean isDisplayTime() { return "displaytime".equals(options); }
}
