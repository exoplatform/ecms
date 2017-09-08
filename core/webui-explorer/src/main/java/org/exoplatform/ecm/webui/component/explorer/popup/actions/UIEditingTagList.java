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
package org.exoplatform.ecm.webui.component.explorer.popup.actions;

import org.exoplatform.commons.utils.LazyPageList;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.ecm.webui.component.explorer.UIJCRExplorer;
import org.exoplatform.services.cms.folksonomy.NewFolksonomyService;
import org.exoplatform.services.jcr.ext.hierarchy.NodeHierarchyCreator;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIGrid;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by The eXo Platform SARL
 * Author : Nguyen Anh Vu
 *          anhvurz90@gmail.com
 * Nov 27, 2009
 * 4:18:12 PM
 */
@ComponentConfig(
    template = "system:/groovy/webui/core/UIGrid.gtmpl"
)
public class UIEditingTagList extends UIGrid {

  public UIEditingTagList() throws Exception {
    super();
    getUIPageIterator().setId("TagIterator");
    configure("name", BEAN_FIELD, ACTIONS);
  }

  private static String[] BEAN_FIELD = {"name"};
  private static String[] ACTIONS = {"EditTag", "RemoveTag"};

  final static public String PUBLIC_TAG_NODE_PATH = "exoPublicTagNode";
  final static public String EXO_TOTAL = "exo:total";

  public void updateGrid() throws Exception {
    NewFolksonomyService newFolksonomyService = getApplicationComponent(NewFolksonomyService.class);
    UIJCRExplorer uiExplorer = getAncestorOfType(UIJCRExplorer.class);
    NodeHierarchyCreator nodeHierarchyCreator = uiExplorer.getApplicationComponent(NodeHierarchyCreator.class);
    String workspace = uiExplorer.getRepository().getConfiguration().getDefaultWorkspaceName();
    int scope = uiExplorer.getTagScope();
    String publicTagNodePath = nodeHierarchyCreator.getJcrPath(PUBLIC_TAG_NODE_PATH);

    List<Node> tags = (scope == NewFolksonomyService.PRIVATE) ?
                      newFolksonomyService.getAllPrivateTags(WCMCoreUtils.getRemoteUser()) :
                      newFolksonomyService.getAllPublicTags(publicTagNodePath, workspace);
    List<TagData> tagDataList = new ArrayList<TagData>();
    List<String> tagPaths = new ArrayList<String>();
    for (Node tag : tags) {
      tagDataList.add(new TagData(tag.getName()));
      tagPaths.add(tag.getPath());
    }
    uiExplorer.getTagPaths().retainAll(tagPaths);

    ListAccess<TagData> tagList = new ListAccessImpl<TagData>(TagData.class, tagDataList);
    LazyPageList<TagData> dataPageList = new LazyPageList<TagData>(tagList, 10);
    getUIPageIterator().setPageList(dataPageList);
  }


  static public class TagData {
    private String tagName;

    public TagData(String tagName) {
      this.tagName = tagName;
    }

    public String getName() { return tagName; }
  }


}
