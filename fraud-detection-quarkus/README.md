Quarkus Kafka Streams Quickstart
========================

This project illustrates how you can build [Apache Kafka Streams](https://kafka.apache.org/documentation/streams) applications using Quarkus.

## Anatomy

This quickstart is made up of the following parts:

* Apache Kafka and ZooKeeper
* _producer_, a Quarkus application that publishes some test data on two Kafka topics: "weather-stations" and "temperature-values"
* _aggregator_, a Quarkus application processing the two topics, using the Kafka Streams API

The _aggregator_ application is the interesting piece; it

* runs a KStreams pipeline, that joins the two topics (on the weather station id),
groups the values by weather station and emits the minimum/maximum temperature value per station to the "temperatures-aggregated" topic
* exposes an HTTP endpoint for getting the current minimum/maximum values
for a given station using Kafka Streams interactive queries.

## Building

To build the _producer_ and _aggregator_ applications, run

```bash
mvn clean package
```

## Running

A Docker Compose file is provided for running all the components.
Start all containers by running

```bash
docker-compose up -d --build
```

Now run an instance of the _debezium/tooling_ image which comes with several useful tools such as _kafkacat_ and _httpie_:

```bash
docker run --tty --rm -i --network ks debezium/tooling:1.0
```

In the tooling container, run _kafkacat_ to examine the results of the streaming pipeline:

```bash
kafkacat -b kafka:9092 -C -o beginning -q \
        -t transactions-aggregated
```

You also can obtain the current aggregated state for a given weather station using _httpie_,
which will invoke an Kafka Streams interactive query for that value:

```bash
http aggregator:8080/transactions/data/0000-0000-0000-0001
```

If that node holds the data for key "0000-0000-0000-0001", you'll get a response like this:

```
TTP/1.1 200 OK
Content-Length: 895
Content-Type: application/json

[
    {
        "amount": 4593,
        "cardHolderName": "Claudio Luppi",
        "cardNumber": "0000-0000-0000-0001",
        "date": "2020-02-24T16:36:36.911",
        "transactionReference": "2407bedc-ee31-4df1-9212-b0662aa8e103"
    },
    {
        "amount": 3033,
        "cardHolderName": "Claudio Luppi",
        "cardNumber": "0000-0000-0000-0001",
        "date": "2020-02-24T16:36:52.912",
        "transactionReference": "8d8fbedb-92b1-4f7f-95bd-99483dfdd471"
    },
    {
        "amount": 5968,
        "cardHolderName": "Claudio Luppi",
        "cardNumber": "0000-0000-0000-0001",
        "date": "2020-02-24T16:37:00.91",
        "transactionReference": "19d153d1-312a-433e-a6d3-2770df381d0d"
    },
    {
        "amount": 7605,
        "cardHolderName": "Claudio Luppi",
        "cardNumber": "0000-0000-0000-0001",
        "date": "2020-02-24T16:37:28.874",
        "transactionReference": "2282c2a5-df14-4586-abe0-7719ca7a8a41"
    },
    {
        "amount": 3415,
        "cardHolderName": "Claudio Luppi",
        "cardNumber": "0000-0000-0000-0001",
        "date": "2020-02-24T16:37:16.876",
        "transactionReference": "d7e38175-4658-494a-83bc-9bd7fefced16"
    }
]

## Running locally

For development purposes it can be handy to run the _producer_ and _aggregator_ applications
directly on your local machine instead of via Docker.
For that purpose, a separate Docker Compose file is provided which just starts Apache Kafka and ZooKeeper,
configured to be accessible from your host system

```bash
# If not present yet:
# export HOSTNAME=<your hostname>

docker-compose -f docker-compose-local.yaml up

mvn quarkus:dev -f producer/pom.xml

mvn quarkus:dev -Dquarkus.http.port=8081 -f aggregator/pom.xml
```
