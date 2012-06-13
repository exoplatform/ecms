CKEDITOR.plugins.add('content',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'content';
			var mypath = this.path;	
			editor.ui.addButton(
				'content.btn',
				{
					label : editor.lang.WCMInsertContentPlugins,
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
	window.open(CKEDITOR.eXoPath+'eXoPlugins/content/content.html?insertContentType=All&viewType=list&currentInstance='+e.name,'WCMGadgetSelector','width=1024,height=600');
}
