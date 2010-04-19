FCKCommands.RegisterCommand( "WCMInsertImage", new FCKDialogCommand("WCMInsertImage", "", FCKConfig.eXoPath + "explorer/explorer.html?Type=Image&Connector=" + eXoPlugin.WCMUtils.getRestContext() + "/wcmImage/", 900, 600) );
var oElement = new FCKToolbarButton( "WCMInsertImage" , FCKLang.WCMInsertImagePlugins);
oElement.IconPath = FCKConfig.eXoPath + "plugins/insertImage/insertImage.gif" ;
FCKToolbarItems.RegisterItem( "WCMInsertImage", oElement );
