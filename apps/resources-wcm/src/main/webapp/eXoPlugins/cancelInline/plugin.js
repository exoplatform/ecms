CKEDITOR.plugins.add('cancelInline',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'cancelInline';
			var mypath = this.path;	
			editor.ui.addButton(
				'cancelInline.btn',
				{
					label : editor.lang.CancelUpdateInline,
					command : 'cancelInline.cmd',
					icon : mypath + '/images/cancel.png'
				}
			);
			var cmd = editor.addCommand('cancelInline.cmd', {exec:cancelUpdate});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);


function cancelUpdate(e){
  var instanceCK = document.getElementById("cke_"+e.name);
  e.fire("blur");  
}
