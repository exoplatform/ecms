FCKCommands.RegisterCommand( "WCMInsertPortalLink", new FCKDialogCommand("WCMInsertPortalLink", FCKLang.WCMInsertPortalLinkDialogTitle, FCKConfig.eXoPath + "plugins/insertPortalLink/insertPortalLink.html", 600, 400) );
var oInsertPortalLink = new FCKToolbarButton("WCMInsertPortalLink", FCKLang.WCMInsertPortalLinkPlugins) ;
oInsertPortalLink.IconPath = FCKConfig.eXoPath + "plugins/insertPortalLink/insertPortalLink.gif" ;
FCKToolbarItems.RegisterItem("WCMInsertPortalLink", oInsertPortalLink) ;