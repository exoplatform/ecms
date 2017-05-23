CKEDITOR.plugins.add('content',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'content';
			var mypath = this.path;	
			editor.ui.addButton(
				'content.btn',
				{
					label : editor.lang.content.WCMInsertContentPlugins,
					command : 'content.cmd',
					icon : mypath + '/images/content.png'
				}
			);
			var cmd = editor.addCommand('content.cmd', {exec:showContentSelector});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);


function showContentSelector(e){	
	var nodePath = null;
	var addressBarContent = document.getElementById('AddressBarControl').children[1].defaultValue;
	var subPath = decodeURIComponent(addressBarContent);
	//add this condition to make the difference between the path of a WebContent which already exists and a new one
    if ( document.getElementsByClassName('addressItem')  && document.getElementsByClassName('addressItem').length > 0) {
    	var addressItem = document.getElementsByClassName('addressItem')["0"].childNodes["0"].data;
        //to avoid the duplication of addressItem in case when trying to extract the nodePath of some drives (like Managed Sites) when access to them from the collaboration drive
	   if ( ! ( addressBarContent.startsWith(addressItem) ) ){
	       nodePath = decodeURIComponent(addressItem + addressBarContent);
	   } else {
	       nodePath = decodeURIComponent(addressBarContent).replace(decodeURIComponent(addressItem), "");
	   }
	} else {
	   nodePath = decodeURIComponent(addressBarContent);
	}
	window.open('/eXoWCMResources/eXoPlugins/content/content.html?insertContentType=All&subPath='+subPath+'&nodePath='+nodePath+'&viewType=list&currentInstance='+e.name+'','WCMGadgetSelector','width=1024,height=700');
	window.popup_opened = true;
}
