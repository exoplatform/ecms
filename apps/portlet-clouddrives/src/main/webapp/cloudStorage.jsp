<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>

<div class="VuetifyApp">
  <div id="cloudStorageApp">
    <script type="text/javascript">
      require(['SHARED/cloudStorage'],
              app => app.init()
      );
    </script>
  </div>
</div>