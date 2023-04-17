CKEDITOR.plugins.add( 'attachFile', {

  // Register the icons. They must match command names.
  icons: 'attachFile',
  lang : ['en','fr','de'],

  // The plugin initialization logic goes inside this method.
  init: function( editor ) {

    editor.addCommand( 'attachFile', {

      // Define the function that will be fired when the command is executed.
      exec: function( editor ) {
        document.dispatchEvent(new CustomEvent('open-activity-attachments'));
      }
    });

    // Create the toolbar button that executes the above command.
    editor.ui.addButton( 'attachFile', {
      label: editor.lang.attachFile.buttonTooltip,
      command: 'attachFile',
      toolbar: 'insert'
    });
  }
});