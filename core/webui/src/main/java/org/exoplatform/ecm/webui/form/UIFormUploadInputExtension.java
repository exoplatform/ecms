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

import org.exoplatform.webui.form.UIFormUploadInput;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Nov 24, 2011  
 */
public class UIFormUploadInputExtension extends UIFormUploadInput {

  protected byte[] byteValue;
  protected String fileName;
  protected String mimeType;
  
  public UIFormUploadInputExtension(String name, String bindingExpression) {
    super(name, bindingExpression);
  }
  
  public UIFormUploadInputExtension(String name, String bindingExpression, int limit) {
    super(name, bindingExpression, limit);
  }
  
  public UIFormUploadInputExtension(String name, String bindingExpression, boolean isAutoUpload) {
    super(name, bindingExpression, isAutoUpload);
  }
  
  public UIFormUploadInputExtension(String name, String bindingExpression, int limit, boolean isAutoUpload) {
    super(name, bindingExpression, limit, isAutoUpload);
  }  

  public void setByteValue(byte[] value) { byteValue = value; }

  public byte[] getByteValue() { return byteValue; }

  public String getFileName() { return fileName; }

  public void setFileName(String fileName) { this.fileName = fileName; }
  
  public void setMimeType(String value) { mimeType = value; }
  
  public String getMimeType() { return mimeType; }  

}
