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
package org.exoplatform.services.cms.clipboard.impl;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.cms.clipboard.ClipboardService;
import org.exoplatform.services.cms.clipboard.jcr.model.ClipboardCommand;
import org.exoplatform.services.wcm.utils.WCMCoreUtils;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jan 28, 2014  
 */
public class ClipboardServiceImpl implements ClipboardService {
  
  private static final String CLIPBOARD_CACHE = "ClipboardServiceCache";
  private static final String CLIPBOARD_CACHE_VIRTUAL = "ClipboardServiceCacheVirtual";
  
  private CacheService cacheService_;
  
  private Map<String, Map<String, ClipboardCommand>> repoLastCommand_;
  
  private int liveTime_;
  private int maxSize_;

  public ClipboardServiceImpl(CacheService cacheService, InitParams initParams) {
    this.cacheService_ = cacheService;
    repoLastCommand_ = new HashMap<String, Map<String, ClipboardCommand>>();
    liveTime_ = Integer.parseInt(initParams.getValueParam("liveTime").getValue());
    maxSize_ = Integer.parseInt(initParams.getValueParam("maxSize").getValue());
  }
  
  @Override
  public void addClipboardCommand(String userId, ClipboardCommand command, boolean isVirtual) {
    ExoCache<String, Set<ClipboardCommand>> virtualCache_ = getVirtualCache();
    ExoCache<String, Set<ClipboardCommand>> cache_ = getCache();
    Map<String, ClipboardCommand> lastCommand_ = getLastCommandMap();
    ExoCache<String, Set<ClipboardCommand>> cache = isVirtual ? virtualCache_ : cache_;
    Set<ClipboardCommand> commands = cache.get(userId);
    if (commands != null) {
      commands.add(command);
    } else {
      commands = new LinkedHashSet<ClipboardCommand>();
      commands.add(command);
      cache.put(userId, commands);
    }
    if (!isVirtual) {
      lastCommand_.put(userId, command);
    }
  }

  @Override
  public ClipboardCommand getLastClipboard(String userId) {
    Map<String, ClipboardCommand> lastCommand_ = getLastCommandMap();
    return lastCommand_.get(userId);
  }

  @Override
  public Set<ClipboardCommand> getClipboardList(String userId, boolean isVirtual) {
    ExoCache<String, Set<ClipboardCommand>> virtualCache_ = getVirtualCache();
    ExoCache<String, Set<ClipboardCommand>> cache_ = getCache();
    
    ExoCache<String, Set<ClipboardCommand>> cache = isVirtual ? virtualCache_ : cache_;
    Set<ClipboardCommand> ret = cache.get(userId);
    return ret != null ? ret : new LinkedHashSet<ClipboardCommand>();
  }

  @Override
  public void clearClipboardList(String userId, boolean isVirtual) {
    ExoCache<String, Set<ClipboardCommand>> virtualCache_ = getVirtualCache();
    ExoCache<String, Set<ClipboardCommand>> cache_ = getCache();
    Map<String, ClipboardCommand> lastCommand_ = getLastCommandMap();
    
    ExoCache<String, Set<ClipboardCommand>> cache = isVirtual ? virtualCache_ : cache_;
    Set<ClipboardCommand> commands = cache.get(userId);
    if (commands != null) {
      commands.clear();
    }
    lastCommand_.remove(userId);
  }
  
  private ExoCache<String, Set<ClipboardCommand>> getVirtualCache() {
    ExoCache<String, Set<ClipboardCommand>> ret = cacheService_.getCacheInstance(CLIPBOARD_CACHE_VIRTUAL + getRepoName());
    ret.setLiveTime(liveTime_);
    ret.setMaxSize(maxSize_);
    return ret;
  }
  
  private ExoCache<String, Set<ClipboardCommand>> getCache() {
    ExoCache<String, Set<ClipboardCommand>> ret = cacheService_.getCacheInstance(CLIPBOARD_CACHE + getRepoName());
    ret.setLiveTime(liveTime_);
    ret.setMaxSize(maxSize_);
    return ret;
  }
  
  private Map<String, ClipboardCommand> getLastCommandMap() {
    Map<String, ClipboardCommand> ret = repoLastCommand_.get(getRepoName());
    if (ret == null) {
      ret = new HashMap<String, ClipboardCommand>();
      repoLastCommand_.put(getRepoName(), ret);
    }
    return ret;
  }
  
  private String getRepoName() {
    return WCMCoreUtils.getRepository().getConfiguration().getName();
  }

}
