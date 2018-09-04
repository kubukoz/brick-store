# brick-store

Project for learning pure functional programming in Scala with the Typelevel ecosystem.

## Development

1. Run postgres (localhost, db: postgres, user: postgres, no password)

You can use docker for this:

`docker run -p 5432:5432 postgres:10.2`

Or the local postgres server. If you installed it with `brew`, do `createuser postgres` and everything else should already be there (assuming the service is running).

2. `sbt<enter>run<enter>`
