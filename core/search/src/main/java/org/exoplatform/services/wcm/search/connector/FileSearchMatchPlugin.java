/*
 * Copyright (C) 2026 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.search.connector;

import org.exoplatform.services.wcm.search.model.FileSearchMatchRequest;
import org.exoplatform.services.wcm.search.model.FileSearchMatches;

/**
 * A contribution to unified search's keyword search of files: another add-on
 * that holds searchable data about files (for instance values typed in a form
 * attached to a file) names the files matching the keyword, and what matched,
 * so that they appear among the Files results.
 * <p>
 * The file search adds the contributed ids to its own query, inside the
 * clause the permissions, paths, favorites, tags and file types filter: a
 * contributed file the user may not read is never returned. The matched
 * fields are attached only to the files the search returns.
 * <p>
 * A contributor is a Spring {@code @Service} with an explicit bean name,
 * looked up by type when a search runs; it carries no business logic and
 * delegates to its add-on's service layer.
 */
public interface FileSearchMatchPlugin {

  /**
   * Finds the files matching a keyword in the contributor's own data, for one
   * user. Called once per keyword search, on the request thread: the
   * contributor bounds its own time and answers an empty result, never an
   * exception, when its backend is unavailable.
   *
   * @param request the user, the keyword, the spaces the search is
   *          restricted to, the number of results wanted and the user's
   *          locale
   * @return the matching file node identifiers, best first, and what matched
   *         for each, never null
   */
  FileSearchMatches search(FileSearchMatchRequest request);

}
