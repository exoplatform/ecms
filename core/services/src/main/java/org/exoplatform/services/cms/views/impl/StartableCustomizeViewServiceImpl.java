package org.exoplatform.services.cms.views.impl;

import java.util.ArrayList;
import java.util.List;

import org.picocontainer.Startable;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.cms.views.CustomizeViewPlugin;
import org.exoplatform.services.cms.views.CustomizeViewService;
import org.exoplatform.services.cms.views.ManageViewService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

/**
 * The Class StartableCustomizeViewServiceImpl is used to add custom views to ManageViewService.
 * Collects CustomizeViewPlugins and inits each plugin after the ManageViewService
 * started and initialized default views.
 */
public class StartableCustomizeViewServiceImpl implements Startable, CustomizeViewService {

  /** The Constant LOG. */
  protected static final Log          LOG     = ExoLogger.getLogger(CustomizeViewService.class);

  /** The view service. */
  protected ManageViewService         viewService;

  /** The CustomizeViewPlugin plugins. */
  protected List<CustomizeViewPlugin> plugins = new ArrayList<>();

  /**
   * Instantiates a new customize view service.
   *
   * @param viewService the view service
   */
  public StartableCustomizeViewServiceImpl(ManageViewService viewService) {
    this.viewService = viewService;
  }

  /**
   * Starts the sevice. Inits each plugin and adds configured templates/views to ManageViewService.
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
    // Nothing
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
