package org.exoplatform.services.wcm.extensions.publication.lifecycle.impl;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform MEA Author : haikel.thamri@exoplatform.com
 */
public class LifecyclesConfig {
  private List<Lifecycle> lifecycles = new ArrayList<Lifecycle>();

  public List<Lifecycle> getLifecycles() {
    return lifecycles;
  }

  public void setActions(List<Lifecycle> lifecycles) {
    this.lifecycles = lifecycles;
  }

  public static class Lifecycle {
    private String      name;

    private String      publicationPlugin;

    private List<State> states = new ArrayList<State>();

    public List<State> getStates() {
      return states;
    }

    public void setStates(List<State> states) {
      this.states = states;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getPublicationPlugin() {
      return publicationPlugin;
    }

    public void setPublicationPlugin(String publicationPlugin) {
      this.publicationPlugin = publicationPlugin;
    }

  }

  public static class State {
    private String       state;

    private String       membership;

    private String       role;

    private List<String> roles;

    public List<String> getRoles() {
      return roles;
    }

    public void setRoles(List<String> roles) {
      this.roles = roles;
    }

    private List<String> memberships;

    public String getState() {
      return state;
    }

    public void setState(String state) {
      this.state = state;
    }

    public String getMembership() {
      return membership;
    }

    public void setMembership(String membership) {
      this.membership = membership;
    }

    public List<String> getMemberships() {
      return memberships;
    }

    public void setMemberships(List<String> memberships) {
      this.memberships = memberships;
    }

    public String getRole() {
      return role;
    }

    public void setRole(String role) {
      this.role = role;
    }

  }

}
