/**
 * Copyright (C) 2009 eXo Platform SAS.
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
(function(gj, base) {
	eXo.webui.UIVerticalSlideTabs = {
	  slideInEffect : function() {
	    if ((parseInt(this.selectedTab.style.height) - 30) > 0) {
	      this.selectedTab.style.height = (parseInt(this.selectedTab.style.height) - 30)
	          + "px";
	      setTimeout("eXo.webui.UIVerticalSlideTabs.slideInEffect()", 3);
	    } else {
	      this.selectedTab.style.height = "0px";
	      this.selectedTab.style.display = "none";
	      delete this.selectedTab;
	      this.clickedTab.style.display = "block";
	      setTimeout("eXo.webui.UIVerticalSlideTabs.slideOutEffect()", 3);
	    }
	  },
	
	  slideOutEffect : function() {
	    if ((parseInt(this.clickedTab.style.height) + 30) < this.clickedTab.scrollHeight) {
	      this.clickedTab.style.height = (parseInt(this.clickedTab.style.height) + 30)
	          + "px";
	      setTimeout("eXo.webui.UIVerticalSlideTabs.slideOutEffect()", 3);
	    } else {
	      this.clickedTab.style.height = this.clickedTab.scrollHeight + "px";
	      delete this.clickedTab;
	    }
	  },
	
	  switchVTab : function(clickedElement) {
	    var uiClickedVTab = gj(clickedElement).parents(".UIVTab:first")[0];
	    var uiClickedVTabContent = gj(uiClickedVTab).children("div.UIVTabContent:first")[0];
	    var uiVerticalSlideTabs = gj(clickedElement).parents(".UIVerticalSlideTabs:first")[0];
	    var uiVTabs = gj(uiVerticalSlideTabs).children("div.UIVTab");
	    for ( var i = 0; i < uiVTabs.length; i++) {
	//      if (eXo.core.DOMUtil.getChildrenByTagName(uiVTabs[i], "div")[0].className == "SelectedTab") {
	        if (gj(uiVTabs[i]).children("div")[0].className == "SelectedTab") {
	        this.selectedTab = gj(uiVTabs[i]).children("div.UIVTabContent:first")[0];
	//        eXo.core.DOMUtil.getChildrenByTagName(uiVTabs[i], "div")[0].className = "NormalTab";
	        gj(uiVTabs[i]).children("div")[0].className = "NormalTab";
	        break;
	      }
	    }
	//    eXo.core.DOMUtil.getChildrenByTagName(uiClickedVTab, "div")[0].className = "SelectedTab";
	    gj(uiClickedVTab).children("div")[0].className = "SelectedTab";
	    this.clickedTab = uiClickedVTabContent;
	    if (this.clickedTab != this.selectedTab) {
	      if (this.selectedTab)
	        this.slideInEffect();
	      else {
	        this.clickedTab.style.display = "block";
	        this.slideOutEffect();
	      }
	    }
	  },
	  /**
	   * Action when user clicks on item of vertical tab
	   * 
	   * @param cleckedElement
	   *          clicked element
	   * @param normalStyle
	   *          a css class indicate normal state
	   * @param selectedStyle
	   *          a css class indicate selected state
	   */
	  onTabClick : function(clickedElement, normalStyle, selectedStyle) {
	    var uiClickedVTab = gj(clickedElement).parents(".UIVTab:first")[0];
	    var uiClickedVTabContent = gj(uiClickedVTab).children("div.UIVTabContent:first")[0];
	    var uiVerticalSlideTabs = gj(clickedElement).parents(".UIVerticalSlideTabs:first")[0];
	    var uiVTab = gj(uiVerticalSlideTabs).children("div.UIVTab");
	
	    if (gj(uiClickedVTab, "div")[0].className == normalStyle) {
	      for ( var i = 0; i < uiVTab.length; i++) {
	//        eXo.core.DOMUtil.getChildrenByTagName(uiVTab[i], "div")[0].className = normalStyle;
	    	  gj(uiVTab[i]).children("div")[0].className = normalStyle;
	        gj(uiVTab[i]).children("div.UIVTabContent:first")[0].style.display = "none";
	      }
	//      eXo.core.DOMUtil.getChildrenByTagName(uiClickedVTab, "div")[0].className = selectedStyle;
	      gj(uiClickedVTab).children("div")[0].className = selectedStyle;
	      uiClickedVTabContent.style.display = "block";
	      // eXo.webui.WebUI.fixHeight(eXo.core.DOMUtil.findFirstDescendantByClass(uiClickedVTabContent,
	      // "div", "ScrollArea"), 'UIWorkspacePanel');
	    } else {
	      gj(uiClickedVTab).children("div")[0].className = normalStyle;
	      uiClickedVTabContent.style.display = "none";
	    }
	  },
	
	  onResize : function(uiVerticalSlideTabs, width, height) {
	    var vTabHeight = 35;
	
	    var uiVTabs = gj(uiVerticalSlideTabs).children("div.UIVTab");
	    var uiVTab = this.getSelectedUIVTab(uiVerticalSlideTabs, "div", "UIVTab");
	    if (uiVTab == null)
	      return;
	
	    if (height != null) {
	      var totalTabHeight = (vTabHeight * uiVTabs.length);
	      var controlArea = gj(uiVTab).find("div.ControlArea:first")[0];
	      var controlAreaHeight = 0;
	      if (controlArea != null)
	        controlAreaHeight = controlArea.offsetHeight;
	      scrollArea = gj(uiVTab).find("div.ScrollArea:first")[0];
	      if (scrollArea != null) {
	        scrollArea.style.height = (height - controlAreaHeight - totalTabHeight - 35)
	            + "px";
	      }
	    }
	
	    if (width != null) {
	      scrollArea.style.width = width + "px";
	    }
	  },
	
	  isSelectedUIVTab : function(uiVtab) {
	    var tabRight = gj(uiVtab).find("div.TabRight:first")[0];
	    var changeIcon = gj(tabRight).children("div")[0];
	    if (changeIcon.className == "ExpandButton")
	      return true;
	    return false;
	  },
	
	  getSelectedUIVTab : function(uiVerticalSlideTabs) {
	    var uiVTab = gj(uiVerticalSlideTabs).children("div.UIVTab");
	    for ( var i = 0; i < uiVTab.length; i++) {
	      if (this.isSelectedUIVTab(uiVTab[i]))
	        return uiVTab[i];
	    }
	    return null;
	  },
	
	  fitParentHeight : function(obj) {
	    this.onResize(obj, null, obj.parentNode.offsetHeight);
	  }
	}
	
	return eXo.webui.UIVerticalSlideTabs;
})(gj, base);