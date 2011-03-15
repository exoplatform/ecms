/***************************************************************************
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
 *
 **************************************************************************/
package org.exoplatform.services.jcr.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.services.jcr.config.QueryHandlerEntry;
import org.exoplatform.services.jcr.impl.core.query.lucene.SearchIndex;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Van Chien
 *          chien.nguyen@exoplatform.com
 * Jul 19, 2010
 */
public class IgnoreAccentIndexer extends SearchIndex {

  public IgnoreAccentIndexer(QueryHandlerEntry queryHandlerConfig, ConfigurationManager cfm) {
    super();
  }

  public Analyzer getTextAnalyzer() {
    return new IgnoreAccentAnalyzer();
  }
}
