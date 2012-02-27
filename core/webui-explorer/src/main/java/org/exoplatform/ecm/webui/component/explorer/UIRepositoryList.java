/***************************************************************************
 * Copyright 2001-2008 The eXo Platform SARL         All rights reserved.  *
 * Please look at license.txt in info directory for more license detail.   *
 **************************************************************************/
package org.exoplatform.ecm.webui.component.explorer;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.jcr.config.RepositoryEntry;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormSelectBox;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Nov 8, 2008
 */

@ComponentConfig (
    lifecycle = UIFormLifecycle.class,
    template =  "app:/groovy/webui/component/explorer/UIRepositoryList.gtmpl",
    events = {
      @EventConfig(listeners = UIRepositoryList.ChangeRepoActionListener.class)
    }
)

public class UIRepositoryList extends UIForm {

  /** The log. */
  private static final Log LOG = ExoLogger.getLogger(UIRepositoryList.class);
  
  public static String      FIELD_SELECTREPO = "selectRepo";

  private String            repoName_;

  private RepositoryService rService;

  /**
   * Contructor
   * @throws Exception
   */
  public UIRepositoryList() throws Exception {
    rService = getApplicationComponent(RepositoryService.class);
    repoName_ = rService.getCurrentRepository().getConfiguration().getName();
  }

  /**
   *
   * @return repository name
   */
  public String getRepository() {
    return repoName_;
  }

  /**
   * Set repository name
   * @param repoName
   */
  public void setRepository(String repoName) {
    repoName_ = repoName;
  }

  /**
   * Initiate data for repository box
   * @param defaultRepo
   * @throws Exception
   */
  public void initRepoList(String defaultRepo) throws Exception {
    UIFormSelectBox uiFormSelectBox = getChild(UIFormSelectBox.class);
    String selectedValue = defaultRepo;
    if (uiFormSelectBox == null) {
      uiFormSelectBox = new UIFormSelectBox(FIELD_SELECTREPO, FIELD_SELECTREPO, null);
      addUIFormInput(uiFormSelectBox);
    } else {
      selectedValue = uiFormSelectBox.getValue();
    }
    uiFormSelectBox.setOptions(getRepoItem());
    uiFormSelectBox.setOnChange("ChangeRepo");
    uiFormSelectBox.setValue(selectedValue);
  }

  /**
   * Use RepositoryService get data of repository
   * @return repositories in ArrayList<SelectItemOption<String>>
   */
  private List<SelectItemOption<String>> getRepoItem() {
    List<SelectItemOption<String>> options = new ArrayList<SelectItemOption<String>>();
    RepositoryEntry repo = null;
    try {
      repo = rService.getCurrentRepository().getConfiguration();
    } catch (RepositoryException e) {
      if (LOG.isWarnEnabled()) {
        LOG.warn(e.getMessage());
      }
    }
    options.add(new SelectItemOption<String>(repo.getName(), repo.getName()));
    return options;
  }

  /*
   * Fire event when changing repository
   */
  static public class ChangeRepoActionListener extends EventListener<UIRepositoryList> {
    public void execute(Event<UIRepositoryList> event) throws Exception {
      UIRepositoryList uiRepositoryList = event.getSource();
      UIDrivesBrowserContainer uiDrivesBrowserContainer = uiRepositoryList.getParent();
      UIDrivesBrowser uiDrivesBrowser = uiDrivesBrowserContainer.getChild(UIDrivesBrowser.class);
      UIFormSelectBox uiFormSelectBox = uiRepositoryList.getUIFormSelectBox(FIELD_SELECTREPO);
      String repoName = uiFormSelectBox.getValue();
      uiFormSelectBox.setValue(repoName);
      uiDrivesBrowser.setRepository(repoName);
      uiRepositoryList.setRepository(repoName);
      event.getRequestContext().addUIComponentToUpdateByAjax(uiDrivesBrowserContainer);
    }
  }
}
