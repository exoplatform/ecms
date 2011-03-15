package org.exoplatform.services.wcm.extensions.publication.context.impl;

import java.util.List;

/**
 * Created by The eXo Platform MEA Author :
 * haikel.thamri@exoplatform.com
 */
public class ContextConfig {
    private List<Context> contexts;

    public List<Context> getContexts() {
  return contexts;
    }

    public void setContexts(List<Context> contexts) {
  this.contexts = contexts;
    }

    public static class Context {
  private String name;
  private String priority;
  private String lifecycle;
  private String membership;
  private List<String> memberships;

  private String path;
  private String nodetype;
  private String site;

  public String getName() {
      return name;
  }

  public void setName(String name) {
      this.name = name;
  }

  public String getPriority() {
      return priority;
  }

  public void setPriority(String priority) {
      this.priority = priority;
  }

  public String getLifecycle() {
      return lifecycle;
  }

  public void setLifecycle(String lifecycle) {
      this.lifecycle = lifecycle;
  }

  public String getMembership() {
      return membership;
  }

  public void setMembership(String membership) {
      this.membership = membership;
  }

  public String getPath() {
      return path;
  }

  public void setPath(String path) {
      this.path = path;
  }

  public String getNodetype() {
      return nodetype;
  }

  public void setNodetype(String nodetype) {
      this.nodetype = nodetype;
  }

  public String getSite() {
      return site;
  }

  public void setSite(String site) {
      this.site = site;
  }

  public List<String> getMemberships() {
      return memberships;
  }

  public void setMemberships(List<String> memberships) {
      this.memberships = memberships;
  }

    }
}
