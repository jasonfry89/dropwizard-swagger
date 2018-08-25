<#-- @ftlvariable name="" type="com.federecio.dropwizard.swagger.SwaggerView" -->
<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Swagger UI</title>
  <link rel="icon" type="image/png" href="${swaggerAssetsPath}/images/favicon-32x32.png" sizes="32x32">
  <link rel="icon" type="image/png" href="${swaggerAssetsPath}/images/favicon-16x16.png" sizes="16x16">
  <link href='${swaggerAssetsPath}/css/typography.css' media='screen' rel='stylesheet' type='text/css'/>
  <link href='${swaggerAssetsPath}/css/reset.css' media='screen' rel='stylesheet' type='text/css'/>
  <link href='${swaggerAssetsPath}/css/screen.css' media='screen' rel='stylesheet' type='text/css'/>
  <link href='${swaggerAssetsPath}/css/reset.css' media='print' rel='stylesheet' type='text/css'/>
  <link href='${swaggerAssetsPath}/css/print.css' media='print' rel='stylesheet' type='text/css'/>
  <script src='${swaggerAssetsPath}/lib/jquery-1.8.0.min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/jquery.slideto.min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/jquery.wiggle.min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/jquery.ba-bbq.min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/handlebars-2.0.0.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/underscore-min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/backbone-min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/swagger-client.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/swagger-ui.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/highlight.7.3.pack.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/jsoneditor.min.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/marked.js' type='text/javascript'></script>
  <script src='${swaggerAssetsPath}/lib/swagger-oauth.js' type='text/javascript'></script>

  <!-- Some basic translations -->
  <!-- <script src='lang/translator.js' type='text/javascript'></script> -->
  <!-- <script src='lang/ru.js' type='text/javascript'></script> -->
  <!-- <script src='lang/en.js' type='text/javascript'></script> -->

  <script type="text/javascript">
    $(function () {
      var url = window.location.search.match(/url=([^&]+)/);
      if (url && url.length > 1) {
        url = decodeURIComponent(url[1]);
      } else {
        url = "http://petstore.swagger.io/v2/swagger.json";
      }

      // Pre load translate...
      if(window.SwaggerTranslator) {
        window.SwaggerTranslator.translate();
      }

      window.swaggerUi = new SwaggerUi({
        url: "${contextPath}/swagger.json",
        dom_id: "swagger-ui-container",
        supportedSubmitMethods: ['get', 'post', 'put', 'delete', 'patch'],
        onComplete: function(swaggerApi, swaggerUi){
          if(typeof initOAuth == "function") {
            initOAuth({
              clientId: "your-client-id",
              clientSecret: "your-client-secret-if-required",
              realm: "your-realms",
              appName: "your-app-name",
              scopeSeparator: ",",
              additionalQueryStringParams: {}
            });
          }

          if(window.SwaggerTranslator) {
            window.SwaggerTranslator.translate();
          }

          $('pre code').each(function(i, e) {
            hljs.highlightBlock(e)
          });

          addApiKeyAuthorization();
        },
        onFailure: function(data) {
          log("Unable to Load SwaggerUI");
        },
        docExpansion: "none",
        jsonEditor: false,
        apisSorter: "alpha",
        defaultModelRendering: 'schema',
        showRequestHeaders: false,
        validatorUrl: null
      });

      const apiKeyInput = $('#input_apiKey');
      const getToken = $('#get-token');
      const loginForm = $('#login');
      const loginCancel = $('#login-cancel');
      const apiListingContainer = $('#swagger-ui-container');
      const errorMessage = $('#error-message');

      getToken.click(function (event) {
          event.preventDefault();
          apiListingContainer.hide();
          loginForm.show();
      });

      loginCancel.click(function (event) {
          event.preventDefault();
          loginForm.hide();
          apiListingContainer.show();
      });

      loginForm.submit(function (event) {
          event.preventDefault();
          login($('#username')[0].value.trim(), $('#password')[0].value.trim());
      });

      function login(username, password){
          const url = window.swaggerUi.buildUrl(location.href, "${loginUrl}");
          $.ajax({
              url: url,
              type: 'POST',
              beforeSend: function (xhr) {
                  xhr.setRequestHeader('username', username);
                  xhr.setRequestHeader('password', password);
              },
              data: {},
              success: function(data){
                  const url = window.swaggerUi.buildUrl(location.href, '${contextPath}/swagger');
                  location.href = url + '?token=' + data;
              },
              error: function () {
                  errorMessage.show();
              }
          });
      }

      function addApiKeyAuthorization(){
        var key = encodeURIComponent(apiKeyInput[0].value);
        if(key && key.trim() !== "") {
            var apiKeyAuth = new SwaggerClient.ApiKeyAuthorization("Authorization", "Bearer " + key, "header");
            window.swaggerUi.api.clientAuthorizations.add("bearer", apiKeyAuth);
            log("added key " + key);
        }
      }

      apiKeyInput.change(addApiKeyAuthorization);

      // https://stackoverflow.com/questions/7731778/get-query-string-parameters-with-jquery
      function getQueryParameter(key) {
        key = key.replace(/[*+?^$.\[\]{}()|\\\/]/g, "\\$&"); // escape RegEx meta chars
        var match = location.search.match(new RegExp("[?&]"+key+"=([^&]+)(&|$)"));
        return match && decodeURIComponent(match[1].replace(/\+/g, " "));
      }

      var tokenParam = getQueryParameter("token");
      if(tokenParam) {
          apiKeyInput.val(tokenParam);
      }

      window.swaggerUi.load();
      addApiKeyAuthorization();

      function log() {
        if ('console' in window) {
          console.log.apply(console, arguments);
        }
      }
  });
  </script>
</head>

<body class="swagger-section">
<div id='header'>
  <div class="swagger-ui-wrap">
    <a id="logo" href="http://swagger.io">swagger</a>
    <form id='api_selector'>
      <div class='input'><input placeholder="http://example.com/api" id="input_baseUrl" name="baseUrl" type="text"/></div>
      <div class='input'><input placeholder="token" id="input_apiKey" name="apiKey" type="text"/></div>
      <div class='input'><a id="explore" href="#" data-sw-translate>Explore</a></div>
      <div class='input'><button id="get-token" data-sw-translate>Get Token</button></div>
    </form>
  </div>
</div>

<form id='login' autocomplete="off">
    <input class="login-input" id="username" type="text" placeholder="USERNAME">
    <input class="login-input" id="password" type="password" placeholder="PASSWORD">
    <p id="error-message" style="display: none; color: red; margin-bottom: 5px">Invalid email or password</p>
    <button class="login-button" id="login-submit" style="background-color: #547f00">Submit</button>
    <button class="login-button" id="login-cancel" style="background-color: dimgrey">Cancel</button>
</form>

<div id="message-bar" class="swagger-ui-wrap" data-sw-translate>&nbsp;</div>
<div id="swagger-ui-container" class="swagger-ui-wrap"></div>
</body>
</html>
