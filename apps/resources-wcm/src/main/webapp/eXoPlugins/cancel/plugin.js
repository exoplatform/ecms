CKEDITOR.plugins.add('cancel',
	{
    lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'cancel';
			var mypath = this.path;	
			editor.ui.addButton(
				'cancel.btn',
				{
					label : editor.lang.CancelUpdateInline,
					command : 'cancel.cmd',
					icon : mypath + '/images/cancel.png'
				}
			);
			var cmd = editor.addCommand('cancel.cmd', {exec:cancelUpdate});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
		}
	}
);


function cancelUpdate(e){
  var instanceCK = document.getElementById("cke_"+e.name);
  e.fire("blur");  
}
