/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.ecm.webui.form;

import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.form.UIFormInputBase;
import org.exoplatform.webui.form.UIFormMultiValueInputSet;
import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 21, 2011  
 */
@ComponentConfig(events = {
    @EventConfig(listeners = UIFormMultiValueInputSet.AddActionListener.class, phase = Phase.DECODE),
    @EventConfig(listeners = UIFormMultiValueInputSet.RemoveActionListener.class, phase = Phase.DECODE)})
public class UIFormUploadMultiValueInputSet extends UIFormMultiValueInputSet {

  public UIFormUploadMultiValueInputSet() throws Exception {
    super();
    setComponentConfig(getClass(), null);
  }
  
  public UIFormUploadMultiValueInputSet(String name, String bindingField) throws Exception
  {
     super(name, bindingField);
     setComponentConfig(getClass(), null);
  }
  
  @SuppressWarnings("unchecked")
  public UIFormInputBase createUIFormInput(int idx) throws Exception {
    UIFormInputBase ret = super.createUIFormInput(idx);
    if (ret instanceof UIFormUploadInput) {
      ((UIFormUploadInput)ret).setAutoUpload(true);
    }
    return ret;
  }

}
