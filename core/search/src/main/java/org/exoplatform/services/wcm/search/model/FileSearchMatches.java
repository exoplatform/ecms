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
package org.exoplatform.services.wcm.search.model;

import java.util.List;
import java.util.Map;

/**
 * What a file search contributor answers.
 *
 * @param fileIds the matching file node identifiers, best first
 * @param matches what matched, per file id; a file may have none
 */
public record FileSearchMatches(List<String> fileIds, Map<String, SearchFieldMatches> matches) {

  /** The answer of a contributor that found nothing or could not search. */
  public static final FileSearchMatches EMPTY = new FileSearchMatches(List.of(), Map.of());

}
