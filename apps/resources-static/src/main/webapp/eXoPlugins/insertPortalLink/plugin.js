CKEDITOR.plugins.add( 'insertPortalLink',
{
	requires : [ 'dialog' ],
	init : function( editor )
	{
		var command = editor.addCommand( 'insertPortalLink.cmd', new CKEDITOR.dialogCommand( 'insertPortalLink' ) );
		command.modes = { wysiwyg:1, source:1 };
		command.canUndo = false;

		editor.ui.addButton( 'insertPortalLink.btn',
			{
				label : 'InsertPortalLink',
				command : 'insertPortalLink.cmd',
				icon : this.path + 'images/insertPortalLink.gif'
			});
		CKEDITOR.insertPortalLink = editor;	
		CKEDITOR.dialog.add( 'insertPortalLink', this.path + 'js/portalLink.js' );
	}
});