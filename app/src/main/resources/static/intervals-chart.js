const buildIntervalsChartOptions = (stats) => {
    const chartHeight = Math.max(360, stats.length * 28);
    const rangeData = stats.map((candidate) => ({
        x: candidate.name,
        y: [candidate.min, candidate.max],
        fillColor: candidate.color,
    }));

    return {
        chart: {
            type: "rangeBar",
            height: chartHeight,
            toolbar: {
                show: false,
            },
            zoom: {
                enabled: false,
            },
            animations: {
                enabled: false,
            },
        },
        series: [
            {
                name: "Plage",
                type: "rangeBar",
                data: rangeData,
            },
        ],
        plotOptions: {
            bar: {
                horizontal: true,
                barHeight: "55%",
                borderRadius: 6,
            },
        },
        stroke: {
            width: 0,
        },
        colors: ["#94a3b8"],
        markers: {
            size: 0,
            strokeWidth: 0,
        },
        xaxis: {
            type: "numeric",
            labels: {
                formatter: (value) => `${(value * 100).toFixed(1)}%`,
            },
        },
        yaxis: {
            type: "category",
        },
        tooltip: {
            shared: false,
            custom: ({dataPointIndex, w}) => {
                const point = w.config.series[0]?.data?.[dataPointIndex];
                if (!point || !Array.isArray(point.y)) {
                    return "";
                }

                const [min, max] = point.y;
                return `
                    <div class="apexcharts-tooltip-custom">
                        <div><strong>${point.x}</strong></div>
                        <div>Min: ${(min * 100).toFixed(1)}%</div>
                        <div>Max: ${(max * 100).toFixed(1)}%</div>
                    </div>
                `;
            },
        },
        legend: {
            position: "top",
            horizontalAlign: "left",
        },
        noData: {
            text: "Pas de donnees sur les 365 derniers jours",
        },
    };
};

const renderIntervalsChart = () => {
    if (typeof ApexCharts !== "function") {
        return;
    }

    const globalIntervalElement = document.querySelector(
        ".poll-intervals-chart-global[data-chart-data-id]",
    );
    if (!globalIntervalElement) {
        return;
    }

    const intervalPayload = getPayloadFromElement(globalIntervalElement);
    if (!intervalPayload?.stats?.length) {
        globalIntervalElement.style.display = "none";
        return;
    }

    const intervalsChart = new ApexCharts(
        globalIntervalElement,
        buildIntervalsChartOptions(intervalPayload.stats),
    );
    intervalsChart.render();
};

