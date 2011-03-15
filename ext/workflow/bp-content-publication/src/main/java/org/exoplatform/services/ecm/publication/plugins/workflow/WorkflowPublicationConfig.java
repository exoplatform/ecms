/***************************************************************************
 * Copyright 2001-2009 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.services.ecm.publication.plugins.workflow;

/**
 * Created by The eXo Platform SARL
 * Author : Ly Dinh Quang
 *          quang.ly@exoplatform.com
 *          xxx5669@gmail.com
 * Dec 31, 2008
 */
public class WorkflowPublicationConfig {
  private String validator;
  private String to_workspace;
  private String destPath;
  private boolean isEditable;
  private String backupPath;
  private String backupWorkflow;
  private boolean destPath_currentFolder;

  public boolean isDestPath_currentFolder() {
    return destPath_currentFolder;
  }

  public void setDestPath_currentFolder(boolean destPath_currentFolder) {
    this.destPath_currentFolder = destPath_currentFolder;
  }

  public String getDestPath() {
    return destPath;
  }

  public void setDestPath(String destPath) {
    this.destPath = destPath;
  }

  public boolean isEditable() {
    return isEditable;
  }

  public void setEditable(boolean isEditable) {
    this.isEditable = isEditable;
  }

  public String getTo_workspace() {
    return to_workspace;
  }

  public void setTo_workspace(String to_workspace) {
    this.to_workspace = to_workspace;
  }

  public String getValidator() {
    return validator;
  }

  public void setValidator(String validator) {
    this.validator = validator;
  }

  public String getBackupPath() {
    return backupPath;
  }

  public void setBackupPath(String backupPath) {
    this.backupPath = backupPath;
  }

  public String getBackupWorkflow() {
    return backupWorkflow;
  }

  public void setBackupWorkflow(String backupWorkflow) {
    this.backupWorkflow = backupWorkflow;
  }
}
