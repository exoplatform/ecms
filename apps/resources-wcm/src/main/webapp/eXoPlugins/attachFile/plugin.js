CKEDITOR.plugins.add( 'attachFile', {

  // Register the icons. They must match command names.
  icons: 'attachFile',
  lang : ['en','fr','de'],

  // The plugin initialization logic goes inside this method.
  init: function( editor ) {
    const isNotesEditorInstance = editor.name && editor.name === 'articleContent' || editor.name === 'notesContent'
    editor.addCommand( 'attachFile', {

      // Define the function that will be fired when the command is executed.
      exec: function( editor ) {
        if (isNotesEditorInstance) {
          document.dispatchEvent(new CustomEvent('open-notes-attachments'));
        } else {
          document.dispatchEvent(new CustomEvent('open-activity-attachments'));
        }
      }
    });

    // Create the toolbar button that executes the above command.
    const toolbar = {
      label: editor.lang.attachFile.buttonTooltip,
      command: 'attachFile',
      toolbar: 'insert'
    };
    editor.ui.addButton( 'attachFile', toolbar);
    if (isNotesEditorInstance) {
      editor.on('instanceReady', function() {
        const buttonElement = document.querySelector('.cke_button__attachfile');
        if (buttonElement) {
          buttonElement.classList.add('cke_button_disabled');
          buttonElement.style.setProperty('pointer-events', 'none');
        }
        const iconElement = document.querySelector('.cke_button__attachfile_icon');
        if (iconElement) {
          iconElement.style.setProperty('background-size', '12px', 'important');
          iconElement.style.setProperty('background-position', '1px 1px', 'important');
        }

        document.dispatchEvent(new CustomEvent('note-file-attach-plugin-button-initialized'))
      });

      document.addEventListener('toggle-attach-button', function(event) {
        const buttonElement = document.querySelector('.cke_button__attachfile');
        if (buttonElement) {
          if (event.detail && event.detail.enable) {
            // If the event detail contains 'enable: true', remove the disabled class
            buttonElement.classList.remove('cke_button_disabled');
            buttonElement.style.removeProperty('pointer-events');
          } else {
            // Otherwise, add the disabled class
            buttonElement.classList.add('cke_button_disabled');
            buttonElement.style.setProperty('pointer-events', 'none');
          }
        }
      });
    }
  }
});