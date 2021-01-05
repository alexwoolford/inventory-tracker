# inventory tracker microservices

It's surprising how much we can learn by watching things that change.

Investors, for example, use satellite images that show how full oil storage containers are in order to predict prices. Economists might look at satellite images of Walmart's parking lots, and compare them to previous years, to predict sales numbers before they're announced at the end of the quarter.

Increased social media usage late at night, for example, is an indicator of depression (see [Predicting Depression via Social Media](http://course.duruofei.com/wp-content/uploads/2015/05/Choudhury_Predicting-Depression-via-Social-Media_ICWSM13.pdf)).

In supply chain, there's a concept called the 'Bullwhip effect' that shows how information distortion occurs up the supply chain: 

![microservices](img/bullwhip-effect.png)

If Procter and Gamble (P&G) got a notification every time a baby shat in its diaper, they could reduce waste by matching the quantities being produced with demand. As it stands, there are stockpiles of inventory at the customers, the retailers, the suppliers, and the wholesalers, that make the demand seem more "lumpy" and volatile from P&G's perspective.

For the electronic component industry, it's possible to get some visibility into the popularity of components by looking at inventory movements at distributors.

In this (hypothetical?) example, we'll show how we can capture changes on Digikey's website using 4 microservices communicating via Kafka:

[//]: # (TODO: convert to Avro)

[//]: # (TODO: change diagram to show which services are Spring, Kafka Streams, etc...)

[//]: # (TODO: add link to slides)

[//]: # (TODO: video)

[//]: # (TODO: discuss event time vs stream time, and show how the event time is passed through)

[//]: # (TODO: be more consistent about style - particularly serialization and properties)

[//]: # (TODO: discuss polyglot coding)

[//]: # (TODO: be more consistent about how properties are managed)

[//]: # (TODO: make it CCloud-friendly, and preferably deployable with Docker Compose)

[//]: # (TODO: get jobs to create topics - assume that auto-topic-creation is disabled)

![microservices](img/microservices.png)

It's worth noting that crawling websites may be against the terms of service for the site. I am not a lawyer. You should seek legal advice before doing this sort of thing. This is a _hypothetical_ example of how someone might track inventory changes to figure out what's being sold.

## generate URL's

Takes a seed URL from the `digikey.seed.url` property, and generates URL's for all the part pages under that seed URL.

## get HTML

Takes a URL from the `url` topic, gets the HTML, and publishes to the `html` topic. This service uses [Selenium](https://www.selenium.dev/documentation/en/) to automate Chrome. While browser automation is heavier, it'll render pages that need to execute Javascript in order to display the content.

## parse HTML

Takes the HTML from a page and outputs JSON-formatted part records to the `part` topic. A single page may emit up to 500 part records.

## calculate deltas

Because the _calculate deltas_ step is stateful, this is done using Kafka Streams. A state store keeps the previous inventory quantity for each part. This state store is backed by Kafka so, in the event the Kafka Streams job is stopped or fails, it can be restarted without losing any state.

![calculate deltas topology](img/calculate-deltas-topology.png)

Here's an example. The previous part record shows 196 units, on-hand, of a part:

    {
      "timestamp": 1598492567811,
      "dpn": "1568-DEV-16996-ND",
      "mpn": "DEV-16996",
      "mfg": "SparkFun Electronics",
      "qoh": 196
    }

The current record, for that part, shows that there are now 194 units:

    {
      "timestamp": 1598492654204,
      "dpn": "1568-DEV-16996-ND",
      "mpn": "DEV-16996",
      "mfg": "SparkFun Electronics",
      "qoh": 194
    }

The on-hand inventory level of this part dropped by 2 units between the page requests. A delta record is published to the `delta` topic:

    {
      "timestamp": 1598492654204,
      "dpn": "1568-DEV-16996-ND",
      "mpn": "DEV-16996",
      "mfg": "SparkFun Electronics",
      "delta": 2
    }

