// 1. Force Dark Mode via YouTube's preference cookie and DOM attribute
document.cookie = "PREF=f6=400; domain=.youtube.com; path=/";
document.documentElement.setAttribute("dark", "true");

// 2. Auto Fullscreen Logic
const attemptFullscreen = () => {
    // Browsers block fullscreen API calls without a user gesture.
    // We bind it to the first interaction (click/touch) or the video play event.
    const enterFS = () => {
        const videoElement = document.querySelector('video');
        const fsButton = document.querySelector('.ytp-fullscreen-button') || videoElement;
        
        if (document.fullscreenElement === null && fsButton) {
            try {
                if (videoElement && videoElement.requestFullscreen) {
                    videoElement.requestFullscreen();
                } else if (fsButton.click) {
                    fsButton.click(); 
                }
            } catch(e) { 
                console.warn("Fullscreen API blocked awaiting user gesture.", e); 
            }
        }
    };

    // Attach to the earliest possible user interactions
    document.body.addEventListener('click', enterFS, { once: true });
    document.body.addEventListener('touchstart', enterFS, { once: true });
    
    const video = document.querySelector('video');
    if (video) {
        video.addEventListener('play', enterFS, { once: true });
    }
};

// Execute once DOM is parsed
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', attemptFullscreen);
} else {
    attemptFullscreen();
}