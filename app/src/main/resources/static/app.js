const getPayloadFromElement = (chartElement) => {
    const chartDataId = chartElement.dataset.chartDataId;
    const dataElement = chartDataId ? document.getElementById(chartDataId) : null;

    if (!dataElement?.textContent) {
        return null;
    }

    return JSON.parse(dataElement.textContent);
};

window.addEventListener("DOMContentLoaded", () => {
    renderTrendChart();
    renderIntervalsChart();
    renderLineCharts();
});
