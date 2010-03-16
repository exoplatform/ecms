	(function() {
		var oExplorer = new Object();
		oExplorer.Execute = function() {
			var width = 800;
			var height = 600;
			var iLeft = ( FCKConfig.ScreenWidth  - width ) / 2;
			var iTop  = ( FCKConfig.ScreenHeight - height ) / 2;
			var sOptions = "toolbar=no,status=no,resizable=yes,dependent=yes,scrollbars=yes";
			sOptions += ",width=" + width ;
			sOptions += ",height=" + height;
			sOptions += ",left=" + iLeft;
			sOptions += ",top=" + iTop;
			var newWindow = window.open( FCKConfig.eXoPath + "explorer/explorer.html?Type=File&Connector=" + eXoPlugin.WCMUtils.getRestContext() + "/fckconnector/jcr/", "eXoExplorer", sOptions );
			newWindow.focus();
		}
		FCKCommands.RegisterCommand( "Explorer", oExplorer );
		var oElement = new FCKToolbarButton( "Explorer" );
		oElement.IconPath = FCKConfig.eXoPath + "plugins/explorer/explorer.jpg";
		FCKToolbarItems.RegisterItem( "Explorer", oElement );
	})();

