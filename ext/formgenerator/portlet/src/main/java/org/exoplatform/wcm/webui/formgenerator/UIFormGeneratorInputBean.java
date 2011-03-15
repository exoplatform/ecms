/*
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
 */
package org.exoplatform.wcm.webui.formgenerator;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * chuong.phan@exoplatform.com, phan.le.thanh.chuong@gmail.com
 * Jun 23, 2009
 */
public class UIFormGeneratorInputBean {

  /** The inputs. */
  private List<UIFormGeneratorInputBean> inputs;

  /** The type. */
  private String type;

  /** The name. */
  private String name;

  /** The value. */
  private String value;

  /** The advanced. */
  private String advanced;

  /** The guideline. */
  private String guideline;

  /** The width. */
  private int width;

  /** The height. */
  private int height;

  /**
   * Gets the width.
   *
   * @return the width
   */
  public int getWidth() {
    return width;
  }

  /**
   * Sets the width.
   *
   * @param width the new width
   */
  public void setWidth(int width) {
    this.width = width;
  }

  /**
   * Gets the height.
   *
   * @return the height
   */
  public int getHeight() {
    return height;
  }

  /**
   * Sets the height.
   *
   * @param height the new height
   */
  public void setHeight(int height) {
    this.height = height;
  }

  /** The mandatory. */
  private boolean mandatory;

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Sets the type.
   *
   * @param type the new type
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * Gets the name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * Sets the name.
   *
   * @param name the new name
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Gets the value.
   *
   * @return the value
   */
  public String getValue() {
    return value;
  }

  /**
   * Sets the value.
   *
   * @param value the new value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * Gets the guideline.
   *
   * @return the guideline
   */
  public String getGuideline() {
    return guideline;
  }

  /**
   * Sets the guideline.
   *
   * @param guideline the new guideline
   */
  public void setGuideline(String guideline) {
    this.guideline = guideline;
  }

  /**
   * Checks if is mandatory.
   *
   * @return true, if is mandatory
   */
  public boolean isMandatory() {
    return mandatory;
  }

  /**
   * Sets the mandatory.
   *
   * @param mandatory the new mandatory
   */
  public void setMandatory(boolean mandatory) {
    this.mandatory = mandatory;
  }

  /**
   * Gets the advanced.
   *
   * @return the advanced
   */
  public String getAdvanced() {
    return advanced;
  }

  /**
   * Sets the advanced.
   *
   * @param advanced the new advanced
   */
  public void setAdvanced(String advanced) {
    this.advanced = advanced;
  }

  /**
   * Gets the inputs.
   *
   * @return the inputs
   */
  public List<UIFormGeneratorInputBean> getInputs() {
    return inputs;
  }

  /**
   * Sets the inputs.
   *
   * @param inputs the new inputs
   */
  public void setInputs(List<UIFormGeneratorInputBean> inputs) {
    this.inputs = inputs;
  }
}
