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

import java.util.Map;

import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.cms.scripts.CmsScript;
import org.exoplatform.services.cms.rss.RSSService;
import org.exoplatform.services.jcr.impl.core.NodeImpl;

public class RSSScript implements CmsScript {
  
  private RepositoryService repositoryService_;
  private RSSService rssService_;
  
  public RSSScript(RepositoryService repositoryService, RSSService rssService) {
		repositoryService_ = repositoryService;
		rssService_ = rssService;
  }
  
  public void execute(Object context) {
    Map variables = (Map) context;
    
    String feedType = (String) context.get("exo:feedType") ;    
    
    if(feedType.equals("rss")) {
    	
    	println("***  RSS FEED BUILDING...   ***");    
			
			rssService_.generateFeed(context);
			
		  println("***  BUILD SUCCESSFULL  ***");  
    
    } else if(feedType.equals("podcast")) {
    
			println("***  PODCAST FEED BUILDING... ***");    

			rssService_.generateFeed(context);

			println("***  BUILD SUCCESSFULL  ***");  
    
    }else if(feedType.equals("video podcast")){
    
    	println("***  VIDEO PODCAST FEED BUILDING... ***");    
			
			rssService_.generateFeed(context);
			
			println("***  BUILD SUCCESSFULL  ***");  
			
    } else {
    	
    	println("***  NO BUILD FEED ACTION DONE ***");    
    	
    }
          
  }

  public void setParams(String[] params) {}

}
