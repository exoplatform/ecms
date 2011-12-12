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
package org.exoplatform.services.cms.actions;

import java.util.Collection;
import java.util.Map;

import javax.jcr.Node;

public interface ActionPlugin {

  public boolean isActionTypeSupported(String actionType);

  public String getExecutableDefinitionName();

  public Collection<String> getActionExecutables(String repository) throws Exception;

  public String getActionExecutableLabel();

  public String getActionExecutable(String actionTypeName) throws Exception;

  public boolean isVariable(String variable) throws Exception;

  public Collection<String> getVariableNames(String actionTypeName) throws Exception;

  public void removeObservation(String repository, String moveName) throws Exception;
  

  public void removeActivationJob(String jobName, String jobGroup, String jobClass) throws Exception;

  @Deprecated
  public void addAction(String actionType,
                        String repository,
                        String srcWorkspace,
                        String srcPath,
                        Map mappings) throws Exception;
  
  public void addAction(String actionType,
                        String srcWorkspace,
                        String srcPath,
                        Map mappings) throws Exception;

  @Deprecated
  public void addAction(String actionType,
                        String repository,
                        String srcWorkspace,
                        String srcPath,
                        boolean isDeep,
                        String[] uuid,
                        String[] nodeTypeNames,
                        Map mappings) throws Exception;
  
  public void addAction(String actionType,
                        String srcWorkspace,
                        String srcPath,
                        boolean isDeep,
                        String[] uuid,
                        String[] nodeTypeNames,
                        Map mappings) throws Exception;  

  @Deprecated
  public void initiateActionObservation(Node actionNode, String repository) throws Exception;
  
  public void initiateActionObservation(Node actionNode) throws Exception;

  @Deprecated
  public void reScheduleActivations(Node actionNode, String repository) throws Exception;
  
  public void reScheduleActivations(Node actionNode) throws Exception;

  public void executeAction(String userId, Node actionNode, Map variables, String repository) throws Exception;

  public void executeAction(String userId, String executable, Map variables, String repository) throws Exception;
  
  public void activateAction(String userId, String executable, Map variables) throws Exception;  

  @Deprecated
  public void activateAction(String userId, String executable, Map variables, String repository) throws Exception;

}
