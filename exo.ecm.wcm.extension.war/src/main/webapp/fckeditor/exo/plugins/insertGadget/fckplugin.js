//InsertGadGet
FCKCommands.RegisterCommand( "WCMInsertGadget", new WCMDialogCommand("WCMInsertGadget", "", FCKConfig.eXoPath + "explorer/explorer.html?Type=Gadget&Thumbnail=true&Connector=" + eXoPlugin.WCMUtils.getRestContext() + "/wcmGadget/&disableUploading=true&disableCreatingFolder=true" , 800, 600) );
var oElement = new FCKToolbarButton( "WCMInsertGadget", FCKLang.WCMInsertGadgetPlugins) ;
oElement.IconPath = FCKConfig.eXoPath + "plugins/insertGadget/insertGadget.gif" ;
FCKToolbarItems.RegisterItem( "WCMInsertGadget", oElement );


	
var Gadgets_CommentsProcessorParser = function(oNode, oContent, index) {
	if (/WCM gadgets/.test(oContent)) {
		//WCM gadgets random"[random]" metadata"[metadata]" thumbnail"[thumbnail]"
		var begin = oContent.indexOf('\/\*') + 'WCM gadgets'.length + 3;
		
		var startRandom = oContent.indexOf('random"', begin) + 'random"'.length;
		var endRandom = oContent.indexOf('"', startRandom);
		var random = oContent.substring(startRandom, endRandom);

		var startMetadata = oContent.indexOf('metadata"', endRandom) + 'metadata"'.length;
		var endMetadata = oContent.indexOf('"', startMetadata);
		var metadata = oContent.substring(startMetadata, endMetadata);
		
		var startUserPrefs = oContent.indexOf('userprefs"', endMetadata) + 'userprefs"'.length;
		var endUserPrefs = oContent.indexOf('"', startUserPrefs);
		var userPrefs = oContent.substring(startUserPrefs, endUserPrefs);
		
		var startThumbnail = oContent.indexOf('thumbnail"', endUserPrefs) + 'thumbnail"'.length;
		var endThumbnail = oContent.indexOf('"', startThumbnail);
		var thumbnail = oContent.substring(startThumbnail, endThumbnail);

		var startUrl = oContent.indexOf('url"', endThumbnail) + 'url"'.length;
		var endUrl = oContent.indexOf('"', startUrl);
		var url = oContent.substring(startUrl, endUrl);
		
		if ( !oNode ) {
			FCK.InsertElement(oNode);
		}
		var oFakeNode = FCK.EditorDocument.createElement( 'IMG' ) ;
		oFakeNode.className = 'FCK__Flash' ; 
		oFakeNode.src = thumbnail;
		oFakeNode.setAttribute( '_fckfakelement', 'true', 0 ) ;
		oFakeNode.setAttribute( '_fckrealelement', FCKTempBin.AddElement( oNode ), 0 ) ;
		oFakeNode.setAttribute( '_fckgadgetnumber', random, 0 ) ;
		oFakeNode.setAttribute( '_fckgadgeturl', url, 0 ) ;
		oFakeNode.setAttribute( '_fckgadgetmetadata', metadata, 0 ) ;
		oFakeNode.setAttribute( '_fckgadgetuserprefs', userPrefs, 0 ) ;
		oFakeNode.setAttribute( '_fckgadgetthumbnail', thumbnail, 0 ) ;
		oNode.parentNode.insertBefore( oFakeNode, oNode ) ;
		oNode.parentNode.removeChild( oNode ) ;
	}
}
FCKCommentsProcessor.AddParser(Gadgets_CommentsProcessorParser);

FCK.ContextMenu.RegisterListener( {
	AddItems : function( menu, tag, tagName ) {
		if ( tagName == 'IMG' && tag.getAttribute( '_fckgadgetnumber' ) ) {
			menu.RemoveAllItems() ;
			menu.AddItem('Edit Gadget', 'Edit Gadget', FCKConfig.eXoPath + "plugins/insertGadget/editGadget.gif" ) ;
		}
	}}
);

FCK.RegisterDoubleClickHandler(editGadget, 'IMG' ) ;
function editGadget(oNode) {
	if (!oNode.getAttribute('_fckgadgetthumbnail'))	return ;
	FCK.Commands.GetCommand('Edit Gadget').Execute();
}