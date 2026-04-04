const toPercent = (value) => `${(value * 100).toFixed(1)}%`;

const trendIcon = (direction) => {
    if (direction === "UP") {
        return {symbol: "&#9650;", cssClass: "trend-up"};
    }
    if (direction === "DOWN") {
        return {symbol: "&#9660;", cssClass: "trend-down"};
    }
    return {symbol: "&mdash;", cssClass: "trend-flat"};
};

const MIN_DEFAULT_TREND_POLLING = 0.05; // 5%

const buildTrendRowHtml = (stat, maxLatest) => {
    const width = maxLatest > 0 ? (stat.latestAvg / maxLatest) * 100 : 0;
    const icon = trendIcon(stat.direction);
    const deltaSign = stat.delta > 0 ? "+" : "";
    const deltaLabel = `${deltaSign}${(stat.delta * 100).toFixed(1)} pts`;

    return `
        <div class="trend-row">
            <div class="trend-header">
                <span class="trend-name">${stat.name}</span>
                <span class="trend-latest">${toPercent(stat.latestAvg)}</span>
            </div>
            <div class="trend-bar-track">
                <div class="trend-bar" style="width:${width.toFixed(1)}%;background:${stat.color};"></div>
            </div>
            <div class="trend-meta">
                <span class="trend-delta ${icon.cssClass}">${icon.symbol} ${deltaLabel}</span>
                <span class="trend-compare">vs ${toPercent(stat.previousAvg)}</span>
            </div>
        </div>
    `;
};

const renderTrendChart = () => {
    const trendElement = document.querySelector(".candidate-trend-chart[data-chart-data-id]");
    if (!trendElement) {
        return;
    }

    const payload = getPayloadFromElement(trendElement);
    const stats = payload?.stats || [];
    if (!stats.length) {
        trendElement.style.display = "none";
        return;
    }

    const defaultVisibleStats = stats.filter((stat) => stat.latestAvg >= MIN_DEFAULT_TREND_POLLING);
    const hiddenCount = stats.length - defaultVisibleStats.length;
    const maxLatest = Math.max(...stats.map((stat) => stat.latestAvg), 0);

    trendElement.innerHTML = `
        <div class="trend-list" aria-label="Evolution des candidats sur ${payload.windowDays} jours"></div>
    `;

    const trendListElement = trendElement.querySelector(".trend-list");
    if (!trendListElement) {
        return;
    }

    let isExpanded = false;

    const renderRows = () => {
        const statsToRender = isExpanded ? stats : defaultVisibleStats;
        trendListElement.innerHTML = statsToRender
            .map((stat) => buildTrendRowHtml(stat, maxLatest))
            .join("");
    };

    renderRows();

    if (hiddenCount > 0) {
        const toggleButton = document.createElement("button");
        toggleButton.type = "button";
        toggleButton.className = "trend-toggle";
        toggleButton.textContent = "voir plus";
        toggleButton.setAttribute("aria-expanded", "false");

        toggleButton.addEventListener("click", () => {
            isExpanded = !isExpanded;
            toggleButton.textContent = isExpanded ? "voir moins" : "voir plus";
            toggleButton.setAttribute("aria-expanded", String(isExpanded));
            renderRows();
        });

        const toggleContainer = document.createElement("div");
        toggleContainer.className = "trend-toggle-container";
        toggleContainer.appendChild(toggleButton);
        trendElement.appendChild(toggleContainer);
    }
};

