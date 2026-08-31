var App = (function() {
  var routes = {
    '':        { render: Home.render,   title: 'E-Ink Manager' },
    'fm':      { render: FM.render,     title: 'Files' },
    'apk':     { render: APK.render,    title: 'APK Manager' },
    'icons':   { render: Icons.render,  title: 'Icon Manager' },
    'settings':{ render: Settings.render, title: 'Settings' }
  };

  function getRoute() {
    var hash = location.hash.slice(1) || '';
    var q = hash.indexOf('?');
    var path = q >= 0 ? hash.slice(0, q) : hash;
    var query = q >= 0 ? hash.slice(q + 1) : '';
    return { path: path, query: query };
  }

  function parseQuery(q) {
    var obj = {};
    if (!q) return obj;
    q.split('&').forEach(function(p) {
      var kv = p.split('=');
      if (kv.length === 2) obj[decodeURIComponent(kv[0])] = decodeURIComponent(kv[1]);
    });
    return obj;
  }

  function navBar(active) {
    var items = [
      { id: '',      icon: ICONS.home,    label: 'Home' },
      { id: 'fm',    icon: ICONS.folder,  label: 'Files' },
      { id: 'apk',   icon: ICONS.phone,   label: 'Apps' },
      { id: 'icons', icon: ICONS.image,   label: 'Icons' },
      { id: 'settings', icon: ICONS.gear, label: 'Settings' }
    ];
    var html = '';
    items.forEach(function(item) {
      var cls = item.id === active ? ' active' : '';
      var href = item.id ? '#/' + item.id : '#/';
      html += '<a href="' + href + '" class="' + cls + '">'
        + svgIcon(item.icon, 22, 22) + '<span>' + item.label + '</span></a>';
    });
    return html;
  }

  function topBar(title, showBack) {
    var html = '';
    if (showBack) {
      var backHash = '#/';
      var r = getRoute();
      if (r.path === 'fm') {
        var qp = parseQuery(r.query);
        if (qp.path) {
          var parts = qp.path.split('/');
          parts.pop();
          backHash = '#/fm?path=' + encodeURIComponent(parts.join('/') || '/');
        }
      }
      html += '<a href="' + backHash + '" class="back">' + svgIcon(ICONS.back, 20, 20) + '</a>';
    }
    html += '<h1>' + title + '</h1>';
    return html;
  }

  function render() {
    var r = getRoute();
    var route = routes[r.path];
    if (!route) route = routes[''];
    var qp = parseQuery(r.query);

    document.getElementById('topbar').innerHTML = topBar(route.title, r.path !== '');
    document.getElementById('nav').innerHTML = navBar(r.path);

    var app = document.getElementById('app');
    app.innerHTML = '<div class="spinner" style="margin:48px auto;display:block"></div>';

    route.render(app, qp);
  }

  function init() {
    window.addEventListener('hashchange', render);
    render();
  }

  return { init: init, getRoute: getRoute, parseQuery: parseQuery };
})();

document.addEventListener('DOMContentLoaded', App.init);
