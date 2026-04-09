# About

Poll data aggregator for the 2027 French presidential election.

[voxpol.fr](https://voxpol.fr)

## Run locally

```bash
./gradlew :app:run --args="--config=dev"
```

## TODO:

- config flag to force fallback on the csv
- cutoff date for old polls
- explain methodology and what's the data behind for the up/down indicators on top of the page
- share link/widget (for each poll?)
- newsletters to alert on new polls
- newsfeed?
- on the trend embed, add option to set up the days window (should be within 7 and 60 days, otherwise default to 21)
