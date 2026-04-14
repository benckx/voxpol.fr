# About

Poll data aggregator for the 2027 French presidential election.

[voxpol.fr](https://voxpol.fr)

## Run locally

```bash
./gradlew :app:run --args="--config=dev"
```

## Libraries

- JSoup for scrapping a Wikipedia page
- Ktor for the backend and web pages rendering
- Ben Manes Caffeine to cache the HTML
- Kotlinx Serialization for JSON parsing
- Koin for dependency injection
- Apex Charts for the charts

## TODO:

- config flag to force fallback on the csv
- cutoff date for old polls
- explain methodology and what's the data behind for the up/down indicators on top of the page
- share link/widget (for each poll?)
- newsletters to alert on new polls
- newsfeed?
- on the trend embed, add option to set up the days window (should be within 7 and 60 days, otherwise default to 21)
