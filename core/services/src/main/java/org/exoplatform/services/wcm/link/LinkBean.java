/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.link;

/**
 * Created by The eXo Platform SAS
 * Author : Phan Le Thanh Chuong
 *          chuong_phan@exoplatform.com
 * Sep 4, 2008
 */
public class LinkBean {

  static final public String SEPARATOR = "@";
  static final public String STATUS = "status=";
  static final public String URL = "url=";

  static final public String STATUS_UNCHECKED = "unchecked";
  static final public String STATUS_ACTIVE = "active";
  static final public String STATUS_BROKEN = "broken";

  private String url;
  private String status;

  public LinkBean(String url, String status) {
    this.url = url;
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String toString() {
    return STATUS + status + SEPARATOR + URL + url;
  }

  public boolean isBroken() {
    return STATUS_BROKEN.equalsIgnoreCase(status);
  }

  public boolean isUnchecked() {
    return STATUS_UNCHECKED.equalsIgnoreCase(status);
  }

  public boolean isActive() {
    return STATUS_ACTIVE.equalsIgnoreCase(status);
  }

  public static LinkBean parse(String link) {
    if (link == null) return new LinkBean("","");
    String[] links = link.split(SEPARATOR);
    if (links.length < 2) return new LinkBean("","");
    String url = links[1].replaceAll(URL, "");
    String status = links[0];
    return new LinkBean(url,status);
  }
}
