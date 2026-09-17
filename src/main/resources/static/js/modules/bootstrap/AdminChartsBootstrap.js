document.querySelectorAll('.js-chart').forEach(canvas => {
    if (canvas.dataset.chartConfig) {
        new Chart(canvas, JSON.parse(canvas.dataset.chartConfig));
    }
});
