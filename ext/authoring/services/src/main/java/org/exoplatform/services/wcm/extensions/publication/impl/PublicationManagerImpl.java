package org.exoplatform.services.wcm.extensions.publication.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
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
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.picocontainer.Startable;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class PublicationManagerImpl implements PublicationManager, Startable {

  private StatesLifecyclePlugin statesLifecyclePlugin;

  private ContextPlugin         contextPlugin;

  private static final Log log         = ExoLogger.getLogger(PublicationManagerImpl.class);

  public void addLifecycle(ComponentPlugin plugin) {
    if (plugin instanceof StatesLifecyclePlugin) {
      statesLifecyclePlugin = (StatesLifecyclePlugin) plugin;
    }
  }

  public void addContext(ComponentPlugin plugin) {
    if (plugin instanceof ContextPlugin) {
      contextPlugin = (ContextPlugin) plugin;
    }
  }

  public void start() {
    // TODO Auto-generated method stub

  }

  public void stop() {
    // TODO Auto-generated method stub

  }

  public Context getContext(String name) {
    if (name != null && contextPlugin != null && contextPlugin.getContextConfig() != null) {
      for (Context context : contextPlugin.getContextConfig().getContexts()) {
        if (name.equals(context.getName())) {
          return context;
        }
      }
    }
    return null;
  }

  public List<Context> getContexts() {
    if (contextPlugin != null && contextPlugin.getContextConfig() != null)
      return contextPlugin.getContextConfig().getContexts();
    return null;
  }

  public Lifecycle getLifecycle(String name) {
    if (name != null && statesLifecyclePlugin != null
        && statesLifecyclePlugin.getLifecyclesConfig() != null) {
      for (Lifecycle lifecycle : statesLifecyclePlugin.getLifecyclesConfig().getLifecycles()) {
        if (name.equals(lifecycle.getName())) {
          return lifecycle;
        }
      }
    }

    return null;
  }

  public List<Lifecycle> getLifecycles() {
    if (statesLifecyclePlugin != null && statesLifecyclePlugin.getLifecyclesConfig() != null)
      return statesLifecyclePlugin.getLifecyclesConfig().getLifecycles();
    return null;
  }

  public List<Lifecycle> getLifecyclesFromUser(String remoteUser, String state) {
    List<Lifecycle> lifecycles = null;

    if (statesLifecyclePlugin != null && statesLifecyclePlugin.getLifecyclesConfig() != null) {
      for (Lifecycle lifecycle : statesLifecyclePlugin.getLifecyclesConfig().getLifecycles()) {
        if (lifecycles == null)
          lifecycles = new ArrayList<Lifecycle>();
        ExoContainer container = ExoContainerContext.getCurrentContainer();
        IdentityRegistry identityRegistry = (IdentityRegistry) container.getComponentInstanceOfType(IdentityRegistry.class);
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

    }
    return lifecycles;
  }
  
  public List<Node> getContents(String fromstate, 
		  String tostate, 
		  String date,
		  String user,
		  String lang,
		  String workspace) throws Exception {
	  
	  WCMComposer wcmComposer = (WCMComposer)ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WCMComposer.class);
	  
	  HashMap<String, String> filters = new HashMap<String, String>();
	  filters.put(WCMComposer.FILTER_MODE, WCMComposer.MODE_EDIT);
	  filters.put(WCMComposer.FILTER_LANGUAGE, lang);
	  StringBuffer query = new StringBuffer("select * from nt:base where publication:currentState='"+fromstate+"'");

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
		  Calendar cal = new GregorianCalendar();
		  cal.add(Calendar.DAY_OF_YEAR, Integer.parseInt(date));
		  query.append(" and publication:startPublishedDate<=TIMESTAMP '"+getISO8601Date(cal)+"'");
		  query.append(" order by publication:startPublishedDate asc");
	  } else {
		  query.append(" order by exo:dateModified desc");
	  }
	  filters.put(WCMComposer.FILTER_QUERY_FULL, query.toString());
	  if (log.isInfoEnabled()) log.info("query="+query.toString());
	  List<Node> nodes = wcmComposer.getContents("repository", workspace, "/", filters, WCMCoreUtils.getUserSessionProvider());
	  
	  return nodes;
  }
  
  private String getISO8601Date(Calendar cal) {
	  DateTime dt = new DateTime(cal.getTimeInMillis()); 
	  DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
	  String str = fmt.print(dt);
	  return str;

  }
}
