package org.exoplatform.services.wcm.extensions.publication;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig.Context;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;

/**
 * PublicationManager is to manage the publication.
 *
 * @LevelAPI Platform
 */
public interface PublicationManager {

  /**
   * Add publication plugin to the publication service.
   *
   * @param plugin The Lifecycle as component plugin to add.
   */
  public void addLifecycle(ComponentPlugin plugin);

  /**
   * Remove publication plugin from the publication service.
   *
   * @param plugin The Lifecycle as component plugin to remove.
   */
  public void removeLifecycle(ComponentPlugin plugin);

  /**
   * Add publication plugin context to the publication service.
   *
   * @param plugin The Context as component plugin to add.
   */
  public void addContext(ComponentPlugin plugin);

  /**
   * Remove publication plugin context from the publication service.
   *
   * @param plugin The Context as component plugin to remove.
   */
  public void removeContext(ComponentPlugin plugin);

  /**
   * Get all the lifecycles which were added to service instances.
   *
   * @return List<Lifecycle>
   */
  public List<Lifecycle> getLifecycles();

  /**
   * Get all the contexts which were added to service instances.
   *
   * @return List<Context>
   */
  public List<Context> getContexts();

  /**
   *  Get a specific context with the given names.
   *
   * @param name The name of the wanted lifecycle.
   * @return Context
   */
  public Context getContext(String name);

  /**
   * Get a specific lifecycle with the given name.
   *
   * @return Lifecycle
   */
  public Lifecycle getLifecycle(String name);

  /**
   * Get all the Lifecycle of a specific user.
   *
   * @param remoteUser The current user of publication service.
   * @param state The current state of the node.
   * @return List<Lifecycle>
   */
  public List<Lifecycle> getLifecyclesFromUser(String remoteUser, String state);

  /**
   * Get all the nodes.
   *
   * @param fromstate The current range state of node.
   * @param tostate The current range state of node.
   * @param date The date of the node.
   * @param user The last user of node.
   * @param lang The node's language.
   * @param workspace The Workspace of the node's location.
   * @return List<Node>
   * @throws Exception
   */
  public List<Node> getContents(String fromstate,
      String tostate,
      String date,
      String user,
      String lang,
      String workspace) throws Exception;
}
