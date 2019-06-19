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
package org.exoplatform.ecm.webui.component.explorer.rightclick.manager;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.ecm.webui.component.explorer.control.filter.*;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.ext.filter.UIExtensionFilter;
import org.exoplatform.webui.ext.filter.UIExtensionFilters;
import org.exoplatform.webui.ext.manager.UIAbstractManager;
import org.exoplatform.webui.ext.manager.UIAbstractManagerComponent;

/**
 * Created by The eXo Platform SARL
 * Author : Hoang Van Hung
 *          hunghvit@gmail.com
 * Aug 6, 2009
 */
@ComponentConfig()
public class RenameManageComponent extends UIAbstractManagerComponent {

 private static final List<UIExtensionFilter> FILTERS
         = Arrays.asList(new UIExtensionFilter[]{new IsNotSpecificFolderNodeFilter(),
                                                 new IsNotInTrashFilter(),
                                                 new IsNotParentLockedWhenRenameFilter(),
                                                 new CanSetPropertyFilter(),
                                                 new IsNotLockedFilter(),
                                                 new IsCheckedOutFilter(),
                                                 new IsNotTrashHomeNodeFilter(),
                                                 new IsNotEditingDocumentFilter()});

  @UIExtensionFilters
  public List<UIExtensionFilter> getFilters() {
    return FILTERS;
  }

  @Override
  public Class<? extends UIAbstractManager> getUIAbstractManagerClass() {
    return null;
  }
}
