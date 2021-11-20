# Timeless

Opinionated, flexible but pragmatic way to try conquer the many inboxes in a Red Hatter's life.

## Setup

- Get a todoist account and grab API token
  - precreate labels: mail, Devel + what you add from the example
- Get a github account and grab API token
- Setup a google project on google cloud platform (public google not red hat as it won't have access to public gmail)
  - Enable GMail api and Google Api
  - Download credentials.json from https://console.cloud.google.com/apis/api/people.googleapis.com/credentials into root of timeless.
- Get pocket account and create an app to get consumer token and use https://reader.fxneumann.de/plugins/oneclickpocket/auth.php to get access token
  

## Build and run timeless

`mvn package`

Copy `timeless-example.yaml` to `timeless.yaml` (put it next to credentials.json)

`java -Dquarkus.config.locations=timeless.yaml -jar target/timeless-0.1.0-SNAPSHOT.jar`


