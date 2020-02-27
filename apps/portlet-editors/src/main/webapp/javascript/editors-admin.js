// TODO deprecated - not used, see vue-app bundle
(function () {
  // getting language of user
  const lang = eXo && eXo.env && eXo.env.portal && eXo.env.portal.language || "en";
  const resourceBundleName = "locale.wcm.editors";
  const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/${resourceBundleName}-${lang}.json`;

  function init() {
    
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
        // init Vue app when locale ressources are ready
        new Vue({
      el: "#editors-admin",
      data: {
        message : "Hello Vue!"
      },
      i18n
    });
   });
  }


  return {"init": init}
})();