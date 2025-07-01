document.addEventListener('DOMContentLoaded', function () {
    const btnR = document.querySelector('.btn-right');
    const btnL = document.querySelector('.btn-left');
    const tracks = document.querySelector('.tracks');
    const specificSelection = document.getElementById('specificSelection');
    const header = document.querySelector('.sticky-header');
    const headerButtons = document.querySelector('.headers');

    if (specificSelection && header && headerButtons) {
        const tracksW = tracks.scrollWidth;

        btnR.addEventListener('click', _ => {
            tracks.scrollBy({
                left: tracksW / 2,
                behavior: 'smooth'
            });
        });

        btnL.addEventListener('click', _ => {
            tracks.scrollBy({
                left: -tracksW / 2,
                behavior: 'smooth'
            });
        });

        const scrollToSpecificTime = () => {
            const specificTimePosition = specificSelection.getBoundingClientRect().top + window.scrollY;
            const headerHeight = header.offsetHeight + headerButtons.offsetHeight; // Add the height of the buttons

            window.scrollTo({
                top: specificTimePosition - headerHeight,
                behavior: 'smooth'
            });
        };

        // Scroll to the specific time adjusted for the sticky header and buttons
        scrollToSpecificTime();
    }

    window.addEventListener('scroll', () => {
        const scrollPosition = window.scrollY;

        if (scrollPosition > 0) {
            header.classList.add('sticky');
        } else {
            header.classList.remove('sticky');
        }
    });

    let observer = new IntersectionObserver(entries => {
        if (!entries[0].isIntersecting) {
            document.body.classList.add('reveal');
        } else {
            document.body.classList.remove('reveal');
        }
    });

    observer.observe(document.querySelector('#top-of-site-pixel-anchor'));
});
