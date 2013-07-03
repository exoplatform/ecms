CKEDITOR.plugins.add('accept',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'accept';
			var mypath = this.path;	
			editor.ui.addButton(
				'accept.btn',
				{
					label : editor.lang.AcceptUpdateInline,
					command : 'accept.cmd',
					icon : mypath + '/images/accept.png'
				}
			);
			var cmd = editor.addCommand('accept.cmd', {exec:acceptUpdate});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);


function acceptUpdate(e){
	window.open(CKEDITOR.eXoPath+'eXoPlugins/content/content.html?insertContentType=All&viewType=list&currentInstance='+e.name,'WCMGadgetSelector','width=1024,height=700');
}
