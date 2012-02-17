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

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 21, 2011  
 */
@ComponentConfig(type = UIFormUploadInputNoUploadButton.class,
                 template = "classpath:groovy/ecm/webui/form/UIFormUploadInputNoUploadButton.gtmpl")
public class UIFormUploadInputNoUploadButton extends UIFormUploadInputExtension {
  
  public UIFormUploadInputNoUploadButton(String name, String bindingExpression) {
    super(name, bindingExpression);
    setComponentConfig(UIFormUploadInputNoUploadButton.class, null);
  }
  
  public UIFormUploadInputNoUploadButton(String name, String bindingExpression, int limit) {
    super (name, bindingExpression, limit);
    setComponentConfig(UIFormUploadInputNoUploadButton.class, null);    
  }
  
  public UIFormUploadInputNoUploadButton(String name, String bindingExpression, boolean isAutoUpload) {
    super (name, bindingExpression, isAutoUpload);
    setComponentConfig(UIFormUploadInputNoUploadButton.class, null);    
  }
  
  public UIFormUploadInputNoUploadButton(String name, String bindingExpression, int limit, boolean isAutoUpload) {
    super(name, bindingExpression, limit, isAutoUpload);
    setComponentConfig(UIFormUploadInputNoUploadButton.class, null);    
  }
    
}
