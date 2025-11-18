(function () {
  if (window.__kafkaDiscordBridgeLoaded) {
    return;
  }
  window.__kafkaDiscordBridgeLoaded = true;

  function notifyChannel() {
    if (window.DiscordBridge && window.DiscordBridge.onChannelUrlChanged) {
      window.DiscordBridge.onChannelUrlChanged(window.location.href);
    }
  }

  function wrapHistory(method) {
    const original = history[method];
    return function () {
      const result = original.apply(this, arguments);
      setTimeout(notifyChannel, 50);
      return result;
    };
  }

  history.pushState = wrapHistory("pushState");
  history.replaceState = wrapHistory("replaceState");
  window.addEventListener("popstate", notifyChannel);
  window.addEventListener("hashchange", notifyChannel);

  document.addEventListener("click", function () {
    setTimeout(notifyChannel, 200);
  });

  setInterval(notifyChannel, 5000);

  notifyChannel();

  if (window.DiscordBridge && window.DiscordBridge.onBootstrapReady) {
    window.DiscordBridge.onBootstrapReady();
  }
})();
