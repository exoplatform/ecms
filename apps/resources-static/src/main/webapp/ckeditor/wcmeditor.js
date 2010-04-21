// Include ckeditor script.
function inc(filename)
{
var body = document.getElementsByTagName('body').item(0);
script = document.createElement('script');
script.src = filename;
script.type = 'text/javascript';
body.appendChild(script)
}

inc("/static/ckeditor/ckeditor.js");

function updateck() {
    for ( instance in CKEDITOR.instances ) {
      if (document.getElementById(instance)==null) { clearInterval(intckeditor); }
      document.getElementById(instance).value = CKEDITOR.instances[instance].getData();
    }
  }

// This code is mandatory to update CKEditor instances as they don't sync natively in Ajax Popup with Chrome and Safari Browser
var intckeditor = setInterval("updateck()", 1000);
