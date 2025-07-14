import './initComponents.js';

//get overridden components if exist
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('FileSearch');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

Vue.use(Vuetify);

export async function formatSearchResult(results) {
  if (results?.length) {
    return await Promise.all(results.map(async (file) => {
      const nodePath = file?.nodePath || '';

      const spacesPrefix = '/spaces/';
      const spaceIndex = nodePath.indexOf(spacesPrefix);
      if (spaceIndex !== -1) {
        const spaceGroupId = nodePath.substring(spaceIndex + spacesPrefix.length).split('/')[0];
        try {
          file.space = await Vue.prototype.$spaceService.getSpaceByGroupId(spaceGroupId);
        } catch (error) {
          console.error(`Failed to fetch space for groupId "${spaceGroupId}":`, error);
        }
      }
      const privateIndex = nodePath.indexOf('/Private/');
      if (privateIndex !== -1) {
        const authorPath = nodePath.substring(0, privateIndex);
        file.author = authorPath.substring(authorPath.lastIndexOf('/') + 1);
      }
      file.filename = nodePath.substring(nodePath.lastIndexOf('/') + 1);
      return file;
    }));
  }
  return results;
}
