package org.exoplatform.services.wcm.extensions.publication.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.wcm.extensions.publication.PublicationManager;
import org.exoplatform.services.wcm.extensions.publication.context.ContextPlugin;
import org.exoplatform.services.wcm.extensions.publication.context.impl.ContextConfig.Context;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.StatesLifecyclePlugin;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.Lifecycle;
import org.exoplatform.services.wcm.extensions.publication.lifecycle.impl.LifecyclesConfig.State;
import org.exoplatform.services.wcm.publication.WCMComposer;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.picocontainer.Startable;

/**
 * PublicationManager is to manage the publication.
 */
public class PublicationManagerImpl implements PublicationManager, Startable {

  private Map<String, Lifecycle> lifecycles = new HashMap<String, Lifecycle>();

  private Map<String, Context>   contexts   = new HashMap<String, Context>();

  private static final Log       LOG        = ExoLogger.getLogger(PublicationManagerImpl.class.getName());

  public void addLifecycle(ComponentPlugin plugin) {
    if (plugin instanceof StatesLifecyclePlugin) {
      if (((StatesLifecyclePlugin) plugin).getLifecyclesConfig() != null) {
        for (Lifecycle l:((StatesLifecyclePlugin) plugin).getLifecyclesConfig().getLifecycles()) {
          if (LOG.isInfoEnabled()) {
            LOG.info("Adding Lifecyle : "+l.getName());
          }
          lifecycles.put(l.getName(), l);
        }
      }
    }
  }

  public void removeLifecycle(ComponentPlugin plugin) {
    if (plugin instanceof StatesLifecyclePlugin) {
    if (((StatesLifecyclePlugin) plugin).getLifecyclesConfig() != null) {
      for (Lifecycle l:((StatesLifecyclePlugin) plugin).getLifecyclesConfig().getLifecycles()) {
        if (lifecycles.get(l.getName())!=null) {
          if (LOG.isInfoEnabled()) {
            LOG.info("Removing Lifecyle : "+l.getName());
          }
          lifecycles.remove(l.getName());
        }
      }
    }
    }
  }

  public void addContext(ComponentPlugin plugin) {
    if (plugin instanceof ContextPlugin) {
      if (((ContextPlugin) plugin).getContextConfig() != null) {
        for (Context c:((ContextPlugin) plugin).getContextConfig().getContexts()) {
          if (LOG.isInfoEnabled()) {
            LOG.info("Adding Context : "+c.getName());
          }
          contexts.put(c.getName(), c);
        }
      }
    }
  }

  public void removeContext(ComponentPlugin plugin) {
    if (plugin instanceof ContextPlugin) {
      if (((ContextPlugin) plugin).getContextConfig() != null) {
        for (Context c:((ContextPlugin) plugin).getContextConfig().getContexts()) {
          if (contexts.get(c.getName())!=null) {
            if (LOG.isInfoEnabled()) {
              LOG.info("Removing Context : "+c.getName());
            }
            contexts.remove(c.getName());
          }
        }
      }
    }
  }

  public void start() {

  }

  public void stop() {

  }

  public Context getContext(String name) {
  if (contexts.containsKey(name))
    return contexts.get(name);

    return null;
  }

  public List<Context> getContexts() {
    return new ArrayList<Context>(contexts.values());
  }

  public Lifecycle getLifecycle(String name) {
    if (lifecycles.containsKey(name))
      return lifecycles.get(name);
    return null;
  }

  public List<Lifecycle> getLifecycles() {
    return new ArrayList<Lifecycle>(lifecycles.values());
  }

  public List<Lifecycle> getLifecyclesFromUser(String remoteUser, String state) {
    List<Lifecycle> lifecycles = null;

    for (Lifecycle lifecycle : getLifecycles()) {
      if (lifecycles == null)
        lifecycles = new ArrayList<Lifecycle>();
      IdentityRegistry identityRegistry = WCMCoreUtils.getService(IdentityRegistry.class);
      Identity identity = identityRegistry.getIdentity(remoteUser);
      for (State state_ : lifecycle.getStates()) {
        if (state.equals(state_.getState())) {
          List<String> memberships = new ArrayList<String>();
          if (state_.getMembership() != null && !"automatic".equals(state_.getMembership())) {
            memberships.add(state_.getMembership());
          }
          if (state_.getMemberships() != null)
            memberships.addAll(state_.getMemberships());
          for (String membership : memberships) {
            String[] membershipTab = membership.split(":");
            if (identity.isMemberOf(membershipTab[1], membershipTab[0])) {
              lifecycles.add(lifecycle);
              break;
            }
          }
        }
      }

    }
    return lifecycles;
  }

  public List<Node> getContents(String fromstate,
                                String tostate,
                                String date,
                                String user,
                                String lang,
                                String workspace) throws Exception {

    WCMComposer wcmComposer = WCMCoreUtils.getService(WCMComposer.class);

    HashMap<String, String> filters = new HashMap<String, String>();
    filters.put(WCMComposer.FILTER_MODE, WCMComposer.MODE_EDIT);
    filters.put(WCMComposer.FILTER_LANGUAGE, lang);
    StringBuffer query = new StringBuffer("select * from nt:base where publication:currentState='"
        + fromstate + "'");

    if (tostate!=null) {
      List<Lifecycle> lifecycles = this.getLifecyclesFromUser(user, tostate);
      if (lifecycles!=null && !lifecycles.isEmpty()) {
        query.append(" and (");
        boolean first = true;
        for (Lifecycle lifecycle:lifecycles) {
          if (!first) query.append(" or ");
          first = false;
          query.append("publication:lifecycle='"+lifecycle.getName()+"'");
        }
        query.append(")");
      } else {
        query.append(" and publication:lifecycle='_no_lifecycle'");
      }
    } else if (user!=null) {
      query.append(" and publication:lastUser='"+user+"'");
    }

    if (date!=null) {
      OffsetDateTime startPublishedDateTime = OffsetDateTime.now().plusDays(Integer.parseInt(date));
      query.append(" and publication:startPublishedDate<=TIMESTAMP '");
      query.append(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(startPublishedDateTime));
      query.append("' order by publication:startPublishedDate asc");
    } else {
      query.append(" order by exo:dateModified desc");
    }
    filters.put(WCMComposer.FILTER_QUERY_FULL, query.toString());
    if (LOG.isDebugEnabled()) LOG.debug("query="+query.toString());
    List<Node> nodes = wcmComposer.getContents(workspace, "/", filters, WCMCoreUtils.getUserSessionProvider());

    return nodes;
  }
}
