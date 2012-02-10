CKEDITOR.plugins.add('insertGadget',
	{
		lang : ['en','fr','vi'],
		init : function(editor) {
			var pluginName = 'insertGadget';
			var mypath = this.path;	
			editor.ui.addButton(
				'insertGadget.btn',
				{
					label : editor.lang.WCMInsertGadgetPlugins,
					command : 'gadget.cmd',
					icon : mypath + '/images/insertGadget.gif'
				}
			);
			var cmd = editor.addCommand('gadget.cmd', {exec:showInsertGadget});
			cmd.modes = {wysiwyg: 1, source: 1};
			cmd.canUndo = false;	
			CKEDITOR.GadgetSelector = editor;
		}
	}
);

function showInsertGadget(e){
	window.open(CKEDITOR.eXoPath+'eXoPlugins/insertGadget/gadget.html?currentInstance='+e.name,'WCMGadgetSelector','width=1024,height=600');
}
