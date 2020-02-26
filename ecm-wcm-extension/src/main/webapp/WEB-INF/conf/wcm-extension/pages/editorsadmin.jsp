<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<div id="editors-admin" class="container-fluid">
  <script>
    require([ "SHARED/editorsadmin" ], function(app) {
      app.init();
    });
  </script>
  {{ message }}
  Test locale: {{ this.$t('DocumentEditors.error.EditorProviderNotRegistered') }}
</div>