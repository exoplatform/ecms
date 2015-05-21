(function(gj, wcm_utils) {
	function Permlink() {
	  this.refreshAction = "";
	
	  this.restContext = eXo.ecm.WCMUtils.getRestContext();
	  this.renameConnector = "/contents/rename";
	  this.cmdGetObjectId = "/getObjectId?";
	  this.cmdRename = "/rename?";
	
	  this.contextObj = "";          // Store value of ECMContextMenu.obj
	  this.objectid = "";            // Store objectid value used to find html elements of rename node
	  this.currentNodePath = "";     // Store current node path
	};
	
	/** Store refresh action **/
	Permlink.prototype.init = function() {
	};
	
	/**
	Init rename form popup.
	**/
	Permlink.prototype.addFileName = function(item) {

		eXo.ecm.WCMUtils.hideContextMenu(item);
	    var contextMenu = gj(item).closest(".UIRightClickPopupMenu, .uiRightClickPopupMenu")[0];
	    contextMenu.style.display = "none";
	    var href = item.getAttribute('href');
	    if (!href) {
	      return;
	    }
	    if (href.indexOf("ajaxGet") != -1) {
	      href = href.replace("ajaxGet", "ajaxPost");
	      this.setAttribute('href', href);
	    }
	    if (!contextMenu.objId) {
	      return;
	    }
	    
	    var objId = contextMenu.objId.replace(/'/g, "\\'");
	    var index = objId.lastIndexOf("/");
	    if (index != -1)
	      objId = objId.substring(index);
      
	    if (href.indexOf("javascript") == -1) {
	      var objIdPermLink = contextMenu.objId.replace(/'/g, "\\'");
	      var indexWorkspace = objIdPermLink.indexOf(":/");
	      if (indexWorkspace != -1)
	        objIdPermLink = objIdPermLink.substring(indexWorkspace + 2);
	      
	      var pathParamIndex = href.indexOf("?path=");
	      if (pathParamIndex != -1) {
	        var path = href.substring(pathParamIndex + 6);
	        var indexDriverName = path.indexOf("/");
	        if (indexDriverName != -1)
	          path = path.substr(0, indexDriverName + 1);
	        href = href.substr(0, pathParamIndex + 6);
	        href = href + path + objIdPermLink;
	      }
        
	      item.setAttribute('href', href);
	      return;
	    } else if (href.indexOf("window.location") != -1) {
	      href = href.substr(0, href.length - 1) + objId + "'";
	    } else if (href.indexOf("ajaxPost") != -1) {
	      href = href.substr(0, href.length - 2) + "', '" + objId + "')";
	    } else {
	      href = href.substr(0, href.length - 2) + objId + "')";
	    }
	
	    eval(href);

	    return false;

	};
	
	eXo.ecm.Permlink = new Permlink();
	return {
		Permlink : eXo.ecm.Permlink
	};
})(gj, wcm_utils);
