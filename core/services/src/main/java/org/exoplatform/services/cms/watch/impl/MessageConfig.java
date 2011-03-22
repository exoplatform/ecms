/*
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
package org.exoplatform.services.cms.watch.impl;

/**
 * Created by The eXo Platform SAS
 * Author : Xuan Hoa Pham
 *          hoapham@exoplatform.com
 *          phamvuxuanhoa@gmail.com
 * Dec 6, 2006
 */
public class MessageConfig {
  private String sender ;
  private String subject ;
  private String mimeType ;
  private String content ;

  public String getContent() { return content; }
  public void setContent(String content) {this.content = content; }

  public String getMimeType() { return mimeType; }
  public void setMimeType(String mimeType) { this.mimeType = mimeType; }

  public String getSender() { return sender; }
  public void setSender(String sender) { this.sender = sender; }

  public String getSubject() { return subject; }
  public void setSubject(String subject) { this.subject = subject;}


}
