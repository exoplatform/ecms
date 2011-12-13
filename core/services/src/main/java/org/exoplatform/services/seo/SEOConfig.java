package org.exoplatform.services.seo;

import java.util.ArrayList;
import java.util.List;

public class SEOConfig {
  private List<String> robotsindex = new ArrayList<String>();
  private List<String> robotsfollow = new ArrayList<String>();
  private List<String> frequency = new ArrayList<String>();

  public List<String> getRobotsIndex () {
    return robotsindex;
  }
  public void setRobotsIndex(List<String> robotsindex_) {
    this.robotsindex = robotsindex_;
  }

  public List<String> getRobotsFollow() {
    return robotsfollow;
  }
  public void setRobotsFollow(List<String> robotsfollow_) {
    this.robotsfollow = robotsfollow_;
  }

  public List<String> getFrequency() {
    return frequency;
  }
  public void setFrequency(List<String> frequency_) {
    this.frequency = frequency_;
  }
}
