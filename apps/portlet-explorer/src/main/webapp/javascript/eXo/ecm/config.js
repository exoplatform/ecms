/**
 * @license Copyright (c) 2003-2015, CKSource - Frederico Knabben. All rights reserved.
 * For licensing, see LICENSE.md or http://ckeditor.com/license
 */

// force compatible version in any case to make sure the editor will initialize
CKEDITOR.env.isCompatible = true;
// force env when using the eXo Android app (the eXo Android app uses a custom user agent which
// is not known by CKEditor and which makes it not initialize the editor)
var userAgent = navigator.userAgent.toLowerCase();
if(userAgent != null && userAgent.indexOf('exo/') == 0 && userAgent.indexOf('(android)') > 0) {
    CKEDITOR.env.mobile = true;
    CKEDITOR.env.chrome = true;
    CKEDITOR.env.gecko = false;
    CKEDITOR.env.webkit = true;
}

CKEDITOR.editorConfig = function( config ) {

    // %REMOVE_START%
    // The configuration options below are needed when running CKEditor from source files.
    config.plugins = 'dialogui,dialog,about,a11yhelp,basicstyles,blockquote,clipboard,panel,floatpanel,menu,contextmenu,button,toolbar,enterkey,entities,popup,filebrowser,floatingspace,listblock,richcombo,format,horizontalrule,htmlwriter,wysiwygarea,image,indent,indentlist,fakeobjects,link,list,maximize,pastetext,pastefromword,removeformat,showborders,sourcearea,specialchar,menubutton,scayt,stylescombo,tab,table,tabletools,undo,wsc,panelbutton,colorbutton,colordialog,autogrow,confighelper';
    CKEDITOR.plugins.addExternal('simpleLink','/commons-extension/eXoPlugins/simpleLink/','plugin.js');
    CKEDITOR.plugins.addExternal('simpleImage','/commons-extension/eXoPlugins/simpleImage/','plugin.js');
    CKEDITOR.plugins.addExternal('suggester','/commons-extension/eXoPlugins/suggester/','plugin.js');
    //TODO we should ensure adding these plugins
    //config.extraPlugins = 'simpleLink,simpleImage,suggester';
    config.skin = 'moono-exo,/commons-extension/ckeditor/skins/moono-exo/';
    // %REMOVE_END%

    // Define changes to default configuration here.
    // For complete reference see:
    // http://docs.ckeditor.com/#!/api/CKEDITOR.config

    // The toolbar groups arrangement.
    config.toolbarGroups = [
        { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
        { name: 'paragraph', groups: [ 'list', 'indent', 'blocks', 'align', 'bidi', 'paragraph' ] }
    ];

    // Remove some buttons provided by the standard plugins, which are
    // not needed in the Standard(s) toolbar.
    config.removeButtons = 'Subscript,Superscript,Cut,Copy,Paste,PasteText,PasteFromWord,Undo,Redo,Scayt,Unlink,Anchor,Table,HorizontalRule,SpecialChar,Maximize,Source,Strike,Outdent,Indent,Format,BGColor,About';

    // Set the most common block elements.
    config.format_tags = 'p;h1;h2;h3;pre';

    // Simplify the dialog windows.
    config.removeDialogTabs = 'image:advanced;link:advanced';

    // Move toolbar below the test area
    config.toolbarLocation = 'bottom';

    // Remove "More colors..." button
    config.colorButton_enableMore = false;

    // style inside the editor
    config.contentsCss = '/social-resources/javascript/eXo/social/ckeditorCustom/contents.css';

    //config.enterMode = CKEDITOR.ENTER_BR;

    config.toolbar = [
        ['Bold','Italic','RemoveFormat',],
        ['-','NumberedList','BulletedList','Blockquote'],
        ['-','simpleLink', 'simpleImage'],
    ] ;

    config.height = 80;

    config.autoGrow_onStartup = true;
    config.autoGrow_minHeight = 80;

    config.placeholder = window.eXo.social.I18n.mentions.defaultMessage;
    config.language = eXo.env.portal.language || 'en';
    config.suggester = {
        suffix: ' ',
        renderMenuItem: '<li data-value="${uid}"><div class="avatarSmall" style="display: inline-block;"><img src="${avatar}"></div>${name} (${uid})</li>',
        renderItem: '<span class="exo-mention">${name}<a href="#" class="remove"><i class="uiIconClose uiIconLightGray"></i></a></span>',
        sourceProviders: ['exo:people']
    };
};
