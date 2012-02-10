CKEDITOR.plugins.add( 'insertPortalLink',
{
	requires : [ 'dialog' ],
	lang : ['en','fr','vi'],
	init : function( editor )
	{
		editor.ui.addButton( 'insertPortalLink.btn',
		{
			label : editor.lang.WCMInsertPortalLinkPlugins,
			command : 'insertPortalLink.cmd',
			icon : this.path + 'images/insertPortalLink.gif'
		});

		var dialog = {
			canUndo : 'false',
			dialogName : 'insertPortalLink.dlg',
			editorFocus : 'false',
			exec : function(editor) {
				editor.openDialog('insertPortalLink.dlg');
				var title = getTheSelectedText(editor);
				editor.titleLink = title;
			}
		}
		
		var command = editor.addCommand( 'insertPortalLink.cmd', dialog);
		command.modes = { wysiwyg:1, source:1 };
		command.canUndo = false;

		CKEDITOR.dialog.add( 'insertPortalLink.dlg', this.path + 'js/portalLink.js' );
	}
});

function getTheSelectedText(editor) {
	var titleLink = "";
	var txtSelection = editor.getSelection();
	var selectedText = '';
	if (CKEDITOR.env.ie) {
		  txtSelection.unlock(true);
		  selectedText = txtSelection.getNative().createRange().text;
	} else {
		  selectedText = txtSelection.getNative().toString();
	}

	if(selectedText && selectedText.length > 0) {
		titleLink = selectedText ;
	}
	return titleLink;
}
