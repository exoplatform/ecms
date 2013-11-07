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

  /**
   * Gets all content nodes.
   *
   * @param fromstate The current state of the content.
   * @param tostate The state by which lifecycles are retrieved from a user.
   * @param date Any given date.
   * The publication dates of returned content nodes are smaller than this given date.
   * @param user The last user who changes the state.
   * @param lang Language of the content nodes.
   * @param workspace The workspace where content nodes are got.
   * @return The list of content nodes.
   * @throws Exception
   */
  public List<Node> getContents(String fromstate,
      String tostate,
      String date,
      String user,
      String lang,
      String workspace) throws Exception;
}
