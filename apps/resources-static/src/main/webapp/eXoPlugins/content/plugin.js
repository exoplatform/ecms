CKEDITOR.plugins.add('content',
	{
		init : function(editor) {
			var pluginName = 'content';
			var mypath = this.path;	
			editor.ui.addButton(
				'content.btn',
				{
					label : "WCM Content Selector",
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

function showContentSelector(e){
	window.open(CKEDITOR.eXoPath+'eXoPlugins/content/content.html?currentInstance='+e.name,'WCMGadgetSelector','width=1024,height=600');
}