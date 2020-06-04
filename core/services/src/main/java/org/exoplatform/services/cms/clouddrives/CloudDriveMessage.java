/*
 * Copyright (C) 2003-2016 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.services.cms.clouddrives;

/**
 * Message should be send to an user as result of Cloud Drive command work.<br>
 * Created by The eXo Platform SAS
 * 
 * @author <a href="mailto:pnedonosko@exoplatform.com">Peter Nedonosko</a>
 * @version $Id: CloudDriveMessage.java 00000 Nov 29, 2014 pnedonosko $
 */
public class CloudDriveMessage {

  /**
   * The Enum Type.
   */
  public enum Type {
    /** The error. */
    ERROR,
    /** The info. */
    INFO,
    /** The warn. */
    WARN
  };

  /** The type. */
  protected final String type;

  /** The text. */
  protected final String text;

  /** The hash code. */
  protected final int    hashCode;

  /**
   * Instantiates a new cloud drive message.
   *
   * @param type the type
   * @param text the text
   */
  public CloudDriveMessage(Type type, String text) {
    this.type = type.name();
    this.text = text;

    int hc = 1;
    hc = hc * 31 + type.hashCode();
    hc = hc * 31 + text.hashCode();
    this.hashCode = hc;
  }

  /**
   * Gets the type.
   *
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * Gets the text.
   *
   * @return the message
   */
  public String getText() {
    return text;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof CloudDriveMessage) {
      CloudDriveMessage other = (CloudDriveMessage) obj;
      return type.equals(other.type) && text.equals(other.text);
    }
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return type + ": " + text;
  }
}
