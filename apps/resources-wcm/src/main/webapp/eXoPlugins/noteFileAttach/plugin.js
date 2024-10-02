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
      toolbar: 'insert',
    });

    editor.on('instanceReady', function() {
      const buttonElement = document.querySelector('.cke_button__notefileattach');
      if (buttonElement) {
        buttonElement.classList.add('cke_button_disabled');
      }
      const iconElement = document.querySelector('.cke_button__notefileattach_icon');
      if (iconElement) {
        iconElement.style.setProperty('background-size', '12px', 'important');
        iconElement.style.setProperty('background-position', '0px 1px', 'important');
      }
      document.dispatchEvent(new CustomEvent('note-file-attach-plugin-button-initialized'))
    });

    document.addEventListener('toggle-attach-button', function(event) {
      const buttonElement = document.querySelector('.cke_button__notefileattach');
      if (buttonElement) {
        if (event.detail && event.detail.enable) {
          // If the event detail contains 'enable: true', remove the disabled class
          buttonElement.classList.remove('cke_button_disabled');
        } else {
          // Otherwise, add the disabled class
          buttonElement.classList.add('cke_button_disabled');
        }
      }
    });

  }
});