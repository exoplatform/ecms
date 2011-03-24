CKEDITOR.plugins.add('content',
	{
		init : function(editor) {
			var pluginName = 'content';
			var mypath = this.path;	
			editor.ui.addButton(
				'content.btn',
				{
					label : "Insert Content Link",
					command : 'content.cmd',
					icon : mypath + '/images/content.jpg'
				}
			);
			var cmd = editor.addCommand('content.cmd', {exec:showContentSelector});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);

CKEDITOR.plugins.add('insertImage',
	{
		init : function(editor) {
			var pluginName = 'insertImage';
			var mypath = this.path;	
			editor.ui.addButton(
				'insertImage.btn',
				{
					label : "Insert Image",
					command : 'insertImage.cmd',
					icon : mypath + '/images/icons/gif.gif'
				}
			);
			var cmd = editor.addCommand('insertImage.cmd', {exec:showImageSelector});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);

function showContentSelector(e){
	window.open(CKEDITOR.eXoPath+'eXoPlugins/content/content.html?insertContentType=All&currentInstance='+e.name,'WCMGadgetSelector','width=1024,height=600');
}

function showImageSelector(e){
	window.open(CKEDITOR.eXoPath+'eXoPlugins/content/content.html?insertContentType=Image&currentInstance='+e.name,'InsertImage','width=1024,height=600');
}
