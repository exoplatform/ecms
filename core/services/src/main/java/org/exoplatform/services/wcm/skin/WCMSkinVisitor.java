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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.exoplatform.portal.resource.SkinConfig;
import org.exoplatform.portal.resource.SkinKey;
import org.exoplatform.portal.resource.SkinVisitor;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 16, 2014
 */
public class WCMSkinVisitor implements SkinVisitor{

    private List<SkinConfig> wcmSiteSkins = new ArrayList<SkinConfig>();
    private List<SkinConfig> wcmSharedSkins = new ArrayList<SkinConfig>();
    
    private String skinName;
    private String moduleName;

    public WCMSkinVisitor(String siteName,String skinName){
      this.skinName = skinName;
      this.moduleName = XSkinService.createModuleName(siteName);
    }

    @Override
    public void visitPortalSkin(Entry<SkinKey, SkinConfig> entry) {
      String name = entry.getKey().getName();
      if (name.equals(this.skinName))
        visit(wcmSharedSkins, entry);
    }

    @Override
    public void visitSkin(Entry<SkinKey, SkinConfig> entry) {
      String module = entry.getKey().getModule();
      String name = entry.getKey().getName();
      if (name.equals(this.skinName) && module.equals(moduleName))
          visit(wcmSiteSkins,entry);
    }

    @Override
    public Collection<SkinConfig> getSkins() {
      List<SkinConfig> skins = new LinkedList<SkinConfig>();
      Comparator<SkinConfig> skinComparator = new Comparator<SkinConfig>(){
        @Override
        public int compare(SkinConfig o1, SkinConfig o2) {
          return o1.getCSSPriority() - o2.getCSSPriority();
        }}; 
            
      Collections.sort(wcmSharedSkins, skinComparator);
      skins.addAll(wcmSharedSkins);
      
      Collections.sort(wcmSiteSkins, skinComparator);
      skins.addAll(wcmSiteSkins);
      
      return skins;
    }

    private void visit(Collection<SkinConfig> skins, Entry<SkinKey, SkinConfig> entry){
      String currentContext = WCMCoreUtils.getRepository().getConfiguration().getName();
      String cssPath = entry.getValue().getCSSPath();
      Map<String,String> params = XSkinService.getSkinParams(cssPath);
      if (cssPath.matches(XSkinService.SKIN_PATH_REGEXP)){
        if (!params.get(XSkinService.CONTEXT_PARAM).equals(currentContext))
          return;
      }
      skins.add(entry.getValue());
    }
}


