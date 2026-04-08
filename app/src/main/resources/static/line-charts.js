const buildLineChartOptions = (payload) => {
    const series = payload.series.map(({name, data}) => ({
        name,
        data,
    }));

    const allValues = payload.series.flatMap(({data}) => data.map(({y}) => y));
    const dataMax = Math.max(...allValues);
    const yMax = Math.ceil(dataMax * 10) / 10;
    const tickAmount = Math.round(yMax * 10);

    return {
        chart: {
            type: "line",
            height: 320,
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
        series,
        colors: payload.series.map(({color}) => color),
        stroke: {
            width: 3,
        },
        markers: {
            size: 4,
            hover: {
                sizeOffset: 2,
            },
        },
        dataLabels: {
            enabled: false,
        },
        legend: {
            position: "top",
            horizontalAlign: "left",
        },
        xaxis: {
            type: "datetime",
            labels: {
                datetimeUTC: false,
            },
        },
        yaxis: {
            min: 0,
            max: yMax,
            tickAmount: tickAmount,
            labels: {
                formatter: (value) => `${(value * 100).toFixed(0)}%`,
            },
        },
        tooltip: {
            shared: true,
            intersect: false,
            x: {
                format: "dd MMM yyyy",
            },
            y: {
                formatter: (value) => `${(value * 100).toFixed(1)}%`,
            },
            custom: ({dataPointIndex, w}) => {
                const points = [];
                w.config.series.forEach((s, idx) => {
                    const point = s.data[dataPointIndex];
                    const value = point?.y;
                    if (value !== undefined) {
                        points.push({
                            name: s.name,
                            value,
                            color: w.config.colors[idx],
                            pollster: point?.pollster || null,
                        });
                    }
                });

                points.sort((a, b) => b.value - a.value);

                const topScoreLevels = [...new Set(points.map((point) => point.value))].slice(0, 2);
                const pollsters = [...new Set(points
                    .map((point) => point.pollster)
                    .filter((pollster) => Boolean(pollster)))];

                const date = new Date(w.config.series[0].data[dataPointIndex].x);
                const formatter = new Intl.DateTimeFormat("fr-FR", {
                    day: "numeric",
                    month: "short",
                    year: "numeric",
                });

                let html = `<div class="apexcharts-tooltip-custom"><div>${formatter.format(date)}</div>`;
                points.forEach((point) => {
                    const candidateName = topScoreLevels.includes(point.value)
                        ? `<strong>${point.name}</strong>`
                        : point.name;
                    html += `<div><span style="color:${point.color}">●</span> ${candidateName}: ${(point.value * 100).toFixed(1)}%</div>`;
                });
                if (pollsters.length > 0) {
                    html += `<div class="tooltip-pollster">Sondeur: ${pollsters.join(" / ")}</div>`;
                }
                html += "</div>";

                return html;
            },
        },
        grid: {
            borderColor: "#e5e7eb",
        },
        noData: {
            text: "Pas assez de sondages",
        },
    };
};

const renderLineCharts = () => {
    if (typeof ApexCharts !== "function") {
        return;
    }

    document.querySelectorAll(".poll-line-chart[data-chart-data-id]").forEach((chartElement) => {
        const payload = getPayloadFromElement(chartElement);
        if (!payload) {
            return;
        }

        const chart = new ApexCharts(chartElement, buildLineChartOptions(payload));
        chart.render();
    });
};

