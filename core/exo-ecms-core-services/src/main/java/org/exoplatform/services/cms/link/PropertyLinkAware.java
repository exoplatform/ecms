/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.services.cms.link;

import java.io.InputStream;
import java.util.Calendar;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.version.VersionException;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          nicolas.filotto@exoplatform.com
 * 1 avr. 2009  
 */
public class PropertyLinkAware extends ItemLinkAware implements Property {

  private final Property property;
  
  public PropertyLinkAware(Session originalSession, String virtualPath, Property property) {
    super(originalSession, virtualPath, property);
    this.property = property;
  }

  public Property getRealProperty() {
    return property;
  }
  
  /**
   * {@inheritDoc}
   */  
  public boolean getBoolean() throws ValueFormatException, RepositoryException {
    return property.getBoolean();
  }

  /**
   * {@inheritDoc}
   */  
  public Calendar getDate() throws ValueFormatException, RepositoryException {
    return property.getDate();
  }

  /**
   * {@inheritDoc}
   */  
  public PropertyDefinition getDefinition() throws RepositoryException {
    return property.getDefinition();
  }

  /**
   * {@inheritDoc}
   */  
  public double getDouble() throws ValueFormatException, RepositoryException {
    return property.getDouble();
  }

  /**
   * {@inheritDoc}
   */  
  public long getLength() throws ValueFormatException, RepositoryException {
    return property.getLength();
  }

  /**
   * {@inheritDoc}
   */  
  public long[] getLengths() throws ValueFormatException, RepositoryException {
    return property.getLengths();
  }

  /**
   * {@inheritDoc}
   */  
  public long getLong() throws ValueFormatException, RepositoryException {
    return property.getLong();
  }

  /**
   * {@inheritDoc}
   */  
  public Node getNode() throws ValueFormatException, RepositoryException {
    return property.getNode();
  }

  /**
   * {@inheritDoc}
   */  
  public InputStream getStream() throws ValueFormatException, RepositoryException {
    return property.getStream();
  }

  /**
   * {@inheritDoc}
   */  
  public String getString() throws ValueFormatException, RepositoryException {
    return property.getString();
  }

  /**
   * {@inheritDoc}
   */  
  public int getType() throws RepositoryException {
    return property.getType();
  }

  /**
   * {@inheritDoc}
   */  
  public Value getValue() throws ValueFormatException, RepositoryException {
    return property.getValue();
  }

  /**
   * {@inheritDoc}
   */  
  public Value[] getValues() throws ValueFormatException, RepositoryException {
    return property.getValues();
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(Value value) throws ValueFormatException,
                                  VersionException,
                                  LockException,
                                  ConstraintViolationException,
                                  RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(Value[] values) throws ValueFormatException,
                                    VersionException,
                                    LockException,
                                    ConstraintViolationException,
                                    RepositoryException {
    property.setValue(values);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(String value) throws ValueFormatException,
                                   VersionException,
                                   LockException,
                                   ConstraintViolationException,
                                   RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(String[] values) throws ValueFormatException,
                                     VersionException,
                                     LockException,
                                     ConstraintViolationException,
                                     RepositoryException {
    property.setValue(values);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(InputStream value) throws ValueFormatException,
                                        VersionException,
                                        LockException,
                                        ConstraintViolationException,
                                        RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(long value) throws ValueFormatException,
                                 VersionException,
                                 LockException,
                                 ConstraintViolationException,
                                 RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(double value) throws ValueFormatException,
                                   VersionException,
                                   LockException,
                                   ConstraintViolationException,
                                   RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(Calendar value) throws ValueFormatException,
                                     VersionException,
                                     LockException,
                                     ConstraintViolationException,
                                     RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(boolean value) throws ValueFormatException,
                                    VersionException,
                                    LockException,
                                    ConstraintViolationException,
                                    RepositoryException {
    property.setValue(value);
  }

  /**
   * {@inheritDoc}
   */  
  public void setValue(Node value) throws ValueFormatException,
                                 VersionException,
                                 LockException,
                                 ConstraintViolationException,
                                 RepositoryException {
    property.setValue(value);
  }
}
