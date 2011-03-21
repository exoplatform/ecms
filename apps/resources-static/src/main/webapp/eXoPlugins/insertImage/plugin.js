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

function showImageSelector(e){
	window.open(CKEDITOR.eXoPath+'eXoPlugins/insertImage/content.html?currentInstance='+e.name,'InsertImage','width=1024,height=600');
}
