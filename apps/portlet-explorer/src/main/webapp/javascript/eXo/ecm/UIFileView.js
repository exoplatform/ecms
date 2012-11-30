function UIFileView() {
	this.openDivs = {};
};

UIFileView.prototype.clickFolder =  function (folderDiv, link, docListId) {
	if (!folderDiv) return;
    folderDiv.className = "FolderCollapsed" == folderDiv.className ? "FolderExpanded" : "FolderCollapsed";
    var docList = document.getElementById(docListId);
    if ("FolderCollapsed" == folderDiv.className) {
      docList.style.display="none";
    } else {
      if (eXo.ecm.UIFileView.openDivs[docListId]) {
        docList.style.display="block";
      } else {
        eval(decodeURIComponent(link));
        eXo.ecm.UIFileView.openDivs[docListId] = docListId;
        docList.style.display="block";        
      }
    }
}

UIFileView.prototype.clearOpenDivs =  function () {
	eXo.ecm.UIFileView.openDivs = {};
}

UIFileView.prototype.clearSideBar =  function () {
	var sidebar = document.getElementById("LeftContainer");
	if (sidebar) {
		sidebar.style.display="block";
	}
	eXo.ecm.ECMUtils.showHideSideBar();
	var workingArea = gj(sidebar).parents(".UIWorkingArea:first")[0];
	var resizeButton = gj(workingArea).find("div.ResizeSideBar:first")[0];
	resizeButton.style.display="none";
}

UIFileView.prototype.showSideBar =  function () {
	var sidebar = document.getElementById("LeftContainer");
	if (sidebar) {
		sidebar.style.display="none";
	}
	eXo.ecm.ECMUtils.showHideSideBar();
	var workingArea = gj(sidebar).parents(".UIWorkingArea:first")[0];
	var resizeButton = gj(workingArea).find("div.ResizeSideBar:first")[0];
	resizeButton.style.display="block";
}


eXo.ecm.UIFileView = new UIFileView();
_module.UIFileView = eXo.ecm.UIFileView;
