/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.services.wcm.skin;

import java.util.*;
import java.util.Map.Entry;

import org.exoplatform.portal.resource.*;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jun
 * 16, 2014
 */
public class WCMSkinVisitor implements SkinVisitor {

  private void visit(Collection<SkinConfig> skins, Entry<SkinKey, SkinConfig> entry) {
    String currentContext = WCMCoreUtils.getRepository().getConfiguration().getName();
    String cssPath = entry.getValue().getCSSPath();
    Map<String, String> params = XSkinService.getSkinParams(cssPath);
    if (cssPath.matches(XSkinService.SKIN_PATH_REGEXP)
        && !params.get(XSkinService.CONTEXT_PARAM).equals(currentContext)) {
      return;
    }
    skins.add(entry.getValue());
  }

  @Override
  public Collection<SkinConfig> getSkins(Set<Entry<SkinKey, SkinConfig>> portalSkins,
                                         Set<Entry<SkinKey, SkinConfig>> skinConfigs) {
    String currentSkin = getCurrentSkin();
    String currentModuleName = getCurrentModuleName();

    List<SkinConfig> wcmSharedSkins = new ArrayList<>();
    for (Entry<SkinKey, SkinConfig> entry : portalSkins) {
      String name = entry.getKey().getName();
      if (name.equals(currentSkin)) {
        visit(wcmSharedSkins, entry);
      }
    }

    List<SkinConfig> wcmSiteSkins = new ArrayList<>();
    for (Entry<SkinKey, SkinConfig> entry : skinConfigs) {
      String module = entry.getKey().getModule();
      String name = entry.getKey().getName();
      if (name.equals(currentSkin) && module.equals(currentModuleName)) {
        visit(wcmSiteSkins, entry);
      }
    }

    List<SkinConfig> skins = new LinkedList<>();
    Comparator<SkinConfig> skinComparator = new Comparator<SkinConfig>() {
      @Override
      public int compare(SkinConfig o1, SkinConfig o2) {
        return o1.getCSSPriority() - o2.getCSSPriority();
      }
    };

    Collections.sort(wcmSharedSkins, skinComparator);
    skins.addAll(wcmSharedSkins);

    Collections.sort(wcmSiteSkins, skinComparator);
    skins.addAll(wcmSiteSkins);
    return skins;
  }

  private String getCurrentModuleName() {
    String currentSiteName = Util.getUIPortal().getName();
    return XSkinService.createModuleName(currentSiteName);
  }

  private String getCurrentSkin() {
    return Util.getUIPortalApplication().getSkin();
  }

}
