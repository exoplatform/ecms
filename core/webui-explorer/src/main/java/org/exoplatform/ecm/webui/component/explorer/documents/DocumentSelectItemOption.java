/*
 * Copyright (C) 2003-2020 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.ecm.webui.component.explorer.documents;

import org.exoplatform.services.cms.documents.NewDocumentTemplateProvider;
import org.exoplatform.webui.core.model.SelectItemOption;

/**
 * The Class DocumentSelectItemOption adds a template provider to SelectItemOption.
 *
 * @param <T> the generic type
 */
public class DocumentSelectItemOption<T> extends SelectItemOption<T> {

  /** The template provider. */
  protected final NewDocumentTemplateProvider templateProvider;
  
  /**
   * Instantiates a new document select item option.
   *
   * @param templateProvider the template provider
   */
  public DocumentSelectItemOption(NewDocumentTemplateProvider templateProvider) {
    super();
    this.templateProvider = templateProvider;
  }


  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param icon the icon
   * @param templateProvider the template provider
   */
  public DocumentSelectItemOption(String label, T value, String icon, NewDocumentTemplateProvider templateProvider) {
    super(label, value, icon);
    this.templateProvider = templateProvider;
  }


  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param desc the desc
   * @param icon the icon
   * @param templateProvider the template provider
   */
  public DocumentSelectItemOption(String label, T value, String desc, String icon, NewDocumentTemplateProvider templateProvider) {
    super(label, value, desc, icon);
    this.templateProvider = templateProvider;
  }

 
  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param desc the desc
   * @param icon the icon
   * @param selected the selected
   * @param templateProvider the template provider
   */
  public DocumentSelectItemOption(String label, T value, String desc, String icon, boolean selected, NewDocumentTemplateProvider templateProvider) {
    super(label, value, desc, icon, selected);
    this.templateProvider = templateProvider;
  }

 
  /**
   * Instantiates a new document select item option.
   *
   * @param label the label
   * @param value the value
   * @param templateProvider the template provider
   */
  public DocumentSelectItemOption(String label, T value, NewDocumentTemplateProvider templateProvider) {
    super(label, value);
    this.templateProvider = templateProvider;
  }

  /**
   * Instantiates a new document select item option.
   *
   * @param value the value
   * @param templateProvider the template provider
   */
  public DocumentSelectItemOption(T value, NewDocumentTemplateProvider templateProvider) {
    super(value);
    this.templateProvider = templateProvider;
  }


  /**
   * Gets the template provider.
   *
   * @return the template provider
   */
  public NewDocumentTemplateProvider getTemplateProvider() {
    return templateProvider;
  }

}
