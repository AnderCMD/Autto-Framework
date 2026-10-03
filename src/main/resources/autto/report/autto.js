// Autto custom behaviour for the Extent Spark report.
(function () {
  'use strict';
  // Pause every other video when one starts playing.
  document.addEventListener('play', function (event) {
    var videos = document.querySelectorAll('.autto-video video');
    for (var i = 0; i < videos.length; i++) {
      if (videos[i] !== event.target) {
        videos[i].pause();
      }
    }
  }, true);
})();
