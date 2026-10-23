// 1. Force Dark Mode
document.cookie = "PREF=f6=400; domain=.youtube.com; path=/";
document.documentElement.setAttribute("dark", "true");

// 2. Inject a script to bypass the gesture block
const script = document.createElement('script');
script.textContent = `
    // Override the browser's Fullscreen API
    Element.prototype.requestFullscreen = function() {
        // Force the player container to fill the browser viewport using CSS
        this.style.position = 'fixed';
        this.style.top = '0';
        this.style.left = '0';
        this.style.width = '100vw';
        this.style.height = '100vh';
        this.style.zIndex = '999999';
        this.style.backgroundColor = 'black';

        // Trick YouTube's player state into thinking fullscreen succeeded
        Object.defineProperty(document, 'fullscreenElement', {
            get: () => this,
            configurable: true
        });

        // Dispatch the event so YouTube updates its UI (changes the icon, etc.)
        document.dispatchEvent(new Event('fullscreenchange'));

        // Silently signal the Android host app to hide the status bar and rotate
        const iframe = document.createElement('iframe');
        iframe.style.display = 'none';
        iframe.src = 'app://enter-fullscreen';
        document.body.appendChild(iframe);
        setTimeout(() => iframe.remove(), 100);

        return Promise.resolve();
    };

    // Automatically call our fake fullscreen when the video starts playing
    document.addEventListener('play', (e) => {
        if (e.target.tagName === 'VIDEO') {
            const player = document.querySelector('.html5-video-player') || e.target.parentElement;
            if (!document.fullscreenElement && player) {
                player.requestFullscreen();
            }
        }
    }, true);
`;
document.documentElement.appendChild(script);
script.remove();