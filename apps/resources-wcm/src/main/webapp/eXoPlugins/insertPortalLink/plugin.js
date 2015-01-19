CKEDITOR.plugins.add( 'insertPortalLink',
{
	requires : [ 'dialog' ],
	lang : ['en','fr','vi'],
	init : function( editor )
	{
		editor.ui.addButton( 'insertPortalLink.btn',
		{
			label : editor.lang.insertPortalLink.WCMInsertPortalLinkPlugins,
			command : 'insertPortalLink.cmd',
			icon : this.path + 'images/insertPortalLink.png'
		});

		var dialog = {
			canUndo : 'false',
			dialogName : 'insertPortalLink.dlg',
			editorFocus : 'false',
			exec : function(editor) {
				editor.openDialog('insertPortalLink.dlg');
				editor.titleLink = editor.getSelection().getSelectedText();
			}
		}
		
		var command = editor.addCommand( 'insertPortalLink.cmd', dialog);
		command.modes = { wysiwyg:1, source:1 };
		command.canUndo = false;

		CKEDITOR.dialog.add( 'insertPortalLink.dlg', this.path + 'js/portalLink.js' );
	}
});

