document.querySelectorAll('.js-manage-chart').forEach(canvas => {
    if (!canvas.dataset.chartConfig) {
        return;
    }

    const chart = new Chart(canvas, JSON.parse(canvas.dataset.chartConfig));
    chart.options.plugins.tooltip = false;
    chart.options.plugins.legend = false;
    chart.update();
});

document.querySelectorAll('.js-manage-chart-big').forEach(canvas => {
    if (canvas.dataset.chartConfig) {
        new Chart(canvas, JSON.parse(canvas.dataset.chartConfig));
    }
});

document.querySelectorAll('.js-toggle-chart').forEach(element => {
    element.addEventListener('click', () => {
        const target = document.querySelector(element.dataset.chartTarget);
        if (target) {
            target.classList.toggle('d-none');
        }
    });
});
