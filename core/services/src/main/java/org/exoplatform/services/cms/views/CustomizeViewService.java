package org.exoplatform.services.cms.views;

import java.util.ArrayList;
import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class CustomizeViewService is used to add custom views to ManageViewService.
 */
public class CustomizeViewService implements Startable {

  /** The Constant LOG. */
  protected static final Log          LOG     = ExoLogger.getLogger(CustomizeViewService.class);

  /** The view service. */
  protected ManageViewService         viewService;

  /** The plugins. */
  protected List<CustomizeViewPlugin> plugins = new ArrayList<>();

  /**
   * Instantiates a new customize view service.
   *
   * @param viewService the view service
   */
  public CustomizeViewService(ManageViewService viewService) {
    this.viewService = viewService;
  }

  /**
   * Start.
   */
  @Override
  public void start() {
    plugins.forEach(plugin -> {
      try {
        plugin.init();
        viewService.getConfiguredTemplates().addAll(plugin.getConfiguredTemplates());
        viewService.getConfiguredViews().addAll(plugin.getConfiguredViews());
      } catch (Exception e) {
        LOG.error("Couldn't initialize CustomizeViewPlugin", e);
      }
    });
  }

  /**
   * Stop.
   */
  @Override
  public void stop() {
  
  }

  
  /**
   * Adds the customize view plugin.
   *
   * @param plugin the plugin
   */
  public void addCustomizeViewPlugin(ComponentPlugin plugin) {
    Class<CustomizeViewPlugin> pclass = CustomizeViewPlugin.class;
    if (pclass.isAssignableFrom(plugin.getClass())) {
      CustomizeViewPlugin customizePlugin = pclass.cast(plugin);
      plugins.add(customizePlugin);
    } else {
      LOG.error("The customizeViewPlugin plugin is not an instance of " + pclass.getName());
    }
  }

}
