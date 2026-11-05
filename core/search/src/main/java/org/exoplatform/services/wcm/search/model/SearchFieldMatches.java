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

/**
 * The line a search result shows when it matched in a contributor's data:
 * an icon with its tooltip, a few « label: value » items, and how many more
 * matched. Every text is localized by the contributor; the card shows it as
 * given.
 */
public class SearchFieldMatches {

  private String                 icon;

  private String                 tooltip;

  private List<SearchFieldMatch> items;

  private int                    more;

  private String                 moreLabel;

  /**
   * An empty line, for the JSON readers.
   */
  public SearchFieldMatches() {
  }

  /**
   * A line.
   *
   * @param icon the icon class shown first
   * @param tooltip the icon's tooltip
   * @param items the matched items shown, at most a few
   * @param more the number of other matched items, not shown
   * @param moreLabel the text shown for the other items
   */
  public SearchFieldMatches(String icon, String tooltip, List<SearchFieldMatch> items, int more, String moreLabel) {
    this.icon = icon;
    this.tooltip = tooltip;
    this.items = items;
    this.more = more;
    this.moreLabel = moreLabel;
  }

  /**
   * @return the icon class shown first
   */
  public String getIcon() {
    return icon;
  }

  /**
   * @param icon the icon class shown first
   */
  public void setIcon(String icon) {
    this.icon = icon;
  }

  /**
   * @return the icon's tooltip
   */
  public String getTooltip() {
    return tooltip;
  }

  /**
   * @param tooltip the icon's tooltip
   */
  public void setTooltip(String tooltip) {
    this.tooltip = tooltip;
  }

  /**
   * @return the matched items shown
   */
  public List<SearchFieldMatch> getItems() {
    return items;
  }

  /**
   * @param items the matched items shown
   */
  public void setItems(List<SearchFieldMatch> items) {
    this.items = items;
  }

  /**
   * @return the number of other matched items
   */
  public int getMore() {
    return more;
  }

  /**
   * @param more the number of other matched items
   */
  public void setMore(int more) {
    this.more = more;
  }

  /**
   * @return the text shown for the other items
   */
  public String getMoreLabel() {
    return moreLabel;
  }

  /**
   * @param moreLabel the text shown for the other items
   */
  public void setMoreLabel(String moreLabel) {
    this.moreLabel = moreLabel;
  }

}
