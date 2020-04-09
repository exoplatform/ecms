package org.exoplatform.services.cms.views;

import org.exoplatform.container.component.ComponentPlugin;

/**
 * The CustomizeViewService interface is used to add/extend views using ComponentPlugins.
 */
public interface CustomizeViewService {
  
  /**
   * Adds the customize view plugin.
   *
   * @param plugin the plugin
   */
  void addCustomizeViewPlugin(ComponentPlugin plugin);

}
