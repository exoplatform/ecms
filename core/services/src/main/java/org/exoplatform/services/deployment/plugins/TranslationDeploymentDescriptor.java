/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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
package org.exoplatform.services.deployment.plugins;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          dongpd@exoplatform.com
 * Oct 12, 2012  
 */
public class TranslationDeploymentDescriptor {
  private List<String> translationNodePaths;
  private boolean overrideExistence;
  
  /**
   * @return the overrideExistence
   */
  public boolean isOverrideExistence() {
    return overrideExistence;
  }
  /**
   * @param overrideExistence the overrideExistence to set
   */
  public void setOverrideExistence(boolean overrideExistence) {
    this.overrideExistence = overrideExistence;
  }
  
  /**
   * @return the translationNodePaths
   */
  public List<String> getTranslationNodePaths() {
    return translationNodePaths;
  }
  /**
   * @param translationNodePaths the translationNodePaths to set
   */
  public void setTranslationNodePaths(List<String> translationNodePaths) {
    this.translationNodePaths = translationNodePaths;
  }
}
