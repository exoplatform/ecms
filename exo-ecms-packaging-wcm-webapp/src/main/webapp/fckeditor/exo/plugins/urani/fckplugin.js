(function(){
		var eUranium = new Object() ;
		eUranium.Name = "Urani" ;
		eUranium.Execute = function() {
			var sumary = FCKeditorAPI.GetInstance("summary");
			var content = FCKeditorAPI.GetInstance("content");
			if (document.selection) {
				var range = FCK.EditorWindow.document.selection.createRange();
				alert(range.text);
			} else  {
				var range = FCK.EditorWindow.getSelection();
				alert(range.getRangeAt(0))
			}
		}
	FCKCommands.RegisterCommand( "Urani", eUranium );
	var oElement = new FCKToolbarButton( "Urani" );
	oElement.IconPath =  FCKConfig.eXoPath + "plugins/urani/urani.gif";
	FCKToolbarItems.RegisterItem( "Urani", oElement );
	})();
