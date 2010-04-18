//Insert Content
FCKCommands.RegisterCommand( "WCMInsertContent", new WCMDialogCommand("WCMInsertContent", "", FCKConfig.eXoPath + "content/content.html?Type=File&Connector=" + eXoPlugin.WCMUtils.getRestContext() + "/wcmDriver/getDrivers?repositoryName=repository&workspaceName=collaboration", 800, 600,null,null,"toolbar=no,status=no,resizable=yes,dependent=yes,scrollbars=yes") );
var oElement = new FCKToolbarButton( "WCMInsertContent" , FCKLang.WCMInsertContent);
oElement.IconPath = FCKConfig.eXoPath + "plugins/insertContent/content.jpg";
FCKToolbarItems.RegisterItem( "WCMInsertContent", oElement );