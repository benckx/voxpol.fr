const buildThresholdChartOptions = (payload, onDataPointIndex) => {
    const allMax = payload.data.map(({max}) => max);
    const dataMax = Math.max(...allMax);
    const yMax = Math.ceil(dataMax * 10) / 10;
    const tickAmount = Math.round(yMax * 10);

    // Move grid lines between band-min and avg series so they render above
    // the white-mask fill but below the avg line.
    const reorderGridLines = (chartInstance) => {
        try {
            const inner = chartInstance.el.querySelector(".apexcharts-inner");
            if (!inner) return;
            const areaSeries = inner.querySelector(".apexcharts-area-series");
            if (!areaSeries) return;
            const avgGroup = areaSeries.querySelector('[data\\:realIndex="2"]');
            if (!avgGroup) return;
            const grid = inner.querySelector(".apexcharts-grid");
            const gridBorders = inner.querySelector(".apexcharts-grid-borders");
            if (grid) areaSeries.insertBefore(grid, avgGroup);
            if (gridBorders) areaSeries.insertBefore(gridBorders, avgGroup);
        } catch (_) {}
    };

    return {
        // Pure area chart — no mixed types. Mixed types break ApexCharts'
        // tooltip left/top positioning (the style attribute is never set).
        chart: {
            type: "area",
            height: 280,
            toolbar: {show: false},
            zoom: {enabled: false},
            animations: {enabled: false},
            events: {
                mounted: reorderGridLines,
                updated: reorderGridLines,
            },
        },
        series: [
            // Band top: fills 0→max with light blue
            {
                name: "band-max",
                data: payload.data.map(({x, max}) => ({x, y: max})),
            },
            // Band bottom: fills 0→min with white, masking the blue below the band
            {
                name: "band-min",
                data: payload.data.map(({x, min}) => ({x, y: min})),
            },
            {
                name: "Seuil moyen",
                data: payload.data.map(({x, avg}) => ({x, y: avg})),
            },
        ],
        colors: ["#93c5fd", "#ffffff", "#1d4ed8"],
        fill: {
            type: ["solid", "solid", "solid"],
            // avg has opacity 0 — no fill, just the stroke line
            opacity: [0.4, 1, 0],
        },
        stroke: {
            // only avg gets a visible stroke
            width: [0, 0, 2],
            curve: "smooth",
        },
        markers: {
            size: [0, 0, 4],
            hover: {sizeOffset: 2},
        },
        dataLabels: {enabled: false},
        legend: {
            customLegendItems: ["Fourchette (min–max)", "Seuil moyen"],
            markers: {
                fillColors: ["#93c5fd", "#1d4ed8"],
            },
            position: "top",
            horizontalAlign: "left",
        },
        xaxis: {
            type: "datetime",
            labels: {
                datetimeUTC: false,
                format: "MMM yy",
            },
        },
        yaxis: {
            min: 0,
            max: yMax,
            tickAmount,
            labels: {
                formatter: (value) => `${(value * 100).toFixed(0)}%`,
            },
        },
        tooltip: {
            shared: true,
            intersect: false,
            custom: ({dataPointIndex, w}) => {
                if (onDataPointIndex) onDataPointIndex(dataPointIndex);
                const maxVal = w.config.series[0].data[dataPointIndex]?.y;
                const minVal = w.config.series[1].data[dataPointIndex]?.y;
                const avgVal = w.config.series[2].data[dataPointIndex]?.y;
                const date = new Date(w.config.series[2].data[dataPointIndex]?.x);
                const formatter = new Intl.DateTimeFormat("fr-FR", {month: "long", year: "numeric"});

                return `<div class="apexcharts-tooltip-custom">
                    <div>${formatter.format(date)}</div>
                    <div>Min : ${(minVal * 100).toFixed(1)}%</div>
                    <div>Moyenne : <strong>${(avgVal * 100).toFixed(1)}%</strong></div>
                    <div>Max : ${(maxVal * 100).toFixed(1)}%</div>
                </div>`;
            },
        },
        grid: {
            borderColor: "#e5e7eb",
        },
        noData: {
            text: "Pas assez de données",
        },
    };
};

const renderThresholdChart = () => {
    if (typeof ApexCharts !== "function") {
        return;
    }

    const chartElement = document.querySelector(".threshold-line-chart[data-chart-data-id]");
    if (!chartElement) {
        return;
    }

    const payload = getPayloadFromElement(chartElement);
    if (!payload?.data?.length) {
        chartElement.style.display = "none";
        return;
    }

    let lastDataPointIndex = -1;
    const chart = new ApexCharts(chartElement, buildThresholdChartOptions(payload, (idx) => {
        lastDataPointIndex = idx;
    }));
    chart.render().then(() => {
        // Kill ApexCharts' built-in "transition: 0.15s ease all" so the
        // tooltip snaps instantly instead of sliding through every point.
        const tooltip = chartElement.querySelector(".apexcharts-tooltip");
        if (tooltip) tooltip.style.transition = "none";
    });

    // ApexCharts does not write left/top on the tooltip for this area-chart
    // configuration, leaving it stuck at 0,0. We override the position after
    // every mousemove — setTimeout(0) guarantees we run after ALL of
    // ApexCharts' own synchronous and rAF-based handlers for that event.
    chartElement.addEventListener("mousemove", (e) => {
        const rect = chartElement.getBoundingClientRect();
        const mouseX = e.clientX - rect.left;
        const mouseY = e.clientY - rect.top;
        setTimeout(() => {
            const tooltip = chartElement.querySelector(".apexcharts-tooltip");
            if (!tooltip) return;
            const tooltipW = tooltip.offsetWidth || 160;
            const tooltipH = tooltip.offsetHeight || 80;
            const chartW = chartElement.offsetWidth;
            // X: follow the mouse, flip left near right edge
            const leftPos = mouseX + tooltipW + 15 > chartW
                ? mouseX - tooltipW - 5
                : mouseX + 15;
            tooltip.style.left = Math.max(0, leftPos) + "px";
            // Y: snap to the avg series data point position
            let topPos = Math.max(0, mouseY - tooltipH / 2);
            if (lastDataPointIndex >= 0) {
                const avgMarkers = chartElement.querySelectorAll('[data\\:realIndex="2"] .apexcharts-marker');
                const marker = avgMarkers[lastDataPointIndex];
                if (marker) {
                    const markerRect = marker.getBoundingClientRect();
                    const markerY = markerRect.top + markerRect.height / 2 - rect.top;
                    topPos = Math.max(0, markerY - tooltipH / 2);
                }
            }
            tooltip.style.top = topPos + "px";
        }, 0);
    });
};
