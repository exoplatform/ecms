package org.exoplatform.services.wcm.extensions.publication;

import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig.Context;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;

/**
 * Manages lifecycle and context of the publication.
 *
 * @LevelAPI Platform
 */
public interface PublicationManager {

  /**
   * Adds definitions of a lifecycle to the publication plugin.
   *
   * @param plugin The component plugin that defines the lifecycle.
   */
  public void addLifecycle(ComponentPlugin plugin);

  /**
   * Removes definitions of a lifecycle from the publication plugin.
   *
   * @param plugin The component plugin that defines the lifecycle.
   */
  public void removeLifecycle(ComponentPlugin plugin);

  /**
   * Adds definitions of a context to the publication plugin.
   *
   * @param plugin The component plugin that defines the context.
   */
  public void addContext(ComponentPlugin plugin);

  /**
   * Removes definitions of a context from the publication plugin.
   *
   * @param plugin The component plugin that defines the context.
   */
  public void removeContext(ComponentPlugin plugin);

  /**
   * Gets all lifecycles.
   *
   * @return The list of lifecycles.
   */
  public List<Lifecycle> getLifecycles();

  /**
   * Gets all contexts.
   *
   * @return The list of contexts.
   */
  public List<Context> getContexts();

  /**
   * Gets a context by a given name.
   *
   * @param name Name of the context.
   * @return The context.
   */
  public Context getContext(String name);

  /**
   * Gets a lifecycle by a given name.
   *
   * @return The lifecycle.
   */
  public Lifecycle getLifecycle(String name);

  /**
   * Gets all lifecycles of a user by a specified state.
   *
   * @param remoteUser The given user.
   * @param state The specified state by which all lifecycles are got.
   * @return The list of lifecycles.
   */
  public List<Lifecycle> getLifecyclesFromUser(String remoteUser, String state);

}
