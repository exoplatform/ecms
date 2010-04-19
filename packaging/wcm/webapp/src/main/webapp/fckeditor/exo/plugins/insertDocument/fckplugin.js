FCKCommands.RegisterCommand( "WCMInsertDocument", new FCKDialogCommand("WCMInsertDocument", "", FCKConfig.eXoPath + "explorer/explorer.html?Type=File&Connector=" + eXoPlugin.WCMUtils.getRestContext() + "/wcmDocument/", 900, 600) );
var oElement = new FCKToolbarButton( "WCMInsertDocument" , FCKLang.WCMInsertDocumentPlugins);
oElement.IconPath = FCKConfig.eXoPath + "plugins/insertDocument/insertDocument.gif" ;
FCKToolbarItems.RegisterItem( "WCMInsertDocument", oElement );
