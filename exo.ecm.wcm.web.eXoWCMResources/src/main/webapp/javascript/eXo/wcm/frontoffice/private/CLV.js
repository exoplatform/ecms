function ContentListViewer() {}

ContentListViewer.prototype.showHideOrderBy = function() {
	var formObj = document.getElementById('UIViewerManagementForm');
	var viewerModeObj = formObj['ViewerMode'];
	var orderXXX = eXo.core.DOMUtil.findDescendantsByClass(formObj, 'tr', 'OrderBlock');			
	viewerModeObj[0].onclick = function() {
		for (var i = 0; i < orderXXX.length; i++) {
			orderXXX[i].style.display = '';
		}
	}
	viewerModeObj[1].onclick = function() {
		for (var i = 0; i < orderXXX.length; i++) {
			orderXXX[i].style.display = 'none';
		}
	}
}

eXo.ecm.CLV = new ContentListViewer();