CKEDITOR.plugins.add( 'noteFileAttach', {

  // Register the icons. They must match command names.
  icons: 'noteFileAttach',
  lang : ['en','fr'],

  // The plugin initialization logic goes inside this method.
  init: function( editor ) {

    editor.addCommand( 'noteFileAttach', {

      // Define the function that will be fired when the command is executed.
      exec: function( editor ) {
        document.dispatchEvent(new CustomEvent('open-notes-attachments'));
      }
    });

    // Create the toolbar button that executes the above command.
    editor.ui.addButton( 'noteFileAttach', {
      label: editor.lang.noteFileAttach.buttonTooltip,
      command: 'noteFileAttach',
      toolbar: 'insert,20'
    });
  }
});