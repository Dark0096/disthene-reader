disthene-reader: cassandra backed metric storage *reader*
=========================================================

## Motivation
This is a "dual" project to [disthene](https://github.com/EinsamHauer/disthene). Though the data written by **disthene** can still be read and plotted by combination of **cyanite** & **graphite-api**, this schema introduces quite some overhead at least caused by serializing/deserializing to/from json. In cases when rendering a graph involves 10s of millions of data points this overhead is quite noticable.
Besides that **graphite-api** as well as original **graphite** rendering could really be a bit faster.
All in all, this project is about read and rendering performance exactly like **disthene** is about write performance.

## What's in
The following APIs are supported:
* /paths API for backward compatibility with **graphite-api** and **cyanite**
* /metrics/find for Grafana
* /metrics
* /render mostly as per Graphite specification version 0.10.0

The functions are mostly per Graphite specification version 0.10.0 with several exceptions below.

The following functions have a different implementation:
* stdev (see https://github.com/graphite-project/graphite-web/issues/986)
* holtWintersForecast
* holtWintersConfidenceBands
* holtWintersConfidenceArea
* holtWintersAberration

The following functions are not implemented:
* smartSummarize
* fallbackSeries
* removeBetweenPercentile
* useSeriesAbove
* removeEmptySeries
* map
* mapSeries
* reduce
* reduceSeries
* identity
* cumulative
* consolidateBy
* changed
* substr
* time
* sin
* randomWalk
* timeFunction
* sinFunction
* randomWalkFunction
* events

## Support Function
Please check [this document](/docs/FUNCTION.md) for the disthene-reader
configuration

## Compiling 

This is a standard Java Gradle project. 

```
gradle jar
```

will most probably do the trick.

## Running
There are a couple of things you will need in runtime

* Cassandra 2.x
* Elasticsearch 2.x
* Grafana using graphite as the datasource

## Configuration
There several configuration files involved
* /etc/disthene-reader/disthene-reader.yaml (location can be changed with -c command line option if needed)
* /etc/disthene-reader/disthene-reader-log4j.xml (location can be changed with -l command line option if needed)

## Quick start for development

You can run the disthene-reader app by using the default setup value as shown below.

```
$ export DISTHENE_HOME=$DISTHENE_INSTALLED_DIR
$ cd $DISTHENE_HOME
$ docker-compose up -d
$ docker exec -it cassandra cqlsh
# create the keyspace and table using $DISTHENE_HOME/infra/cassandra/2.x/metric.cql
$ -c $DISTHENE_HOME/config/disthene-reader-sample.yaml -l $DISTHENE_HOME/config/disthene-reader-log4j-sample.xml -t $DISTHENE_HOME/config/throttling-sample.yaml   
```

## Commit convention
This project uses the git conventional commit rule provided by [conventional commits](https://www.conventionalcommits.org/en/v1.0.0-beta.4/)

## Configuration
Please check [this document](/docs/CONFIGURATION.md) for the disthene-reader configuration                                                                

## License

The MIT License (MIT)

Copyright (C) 2015 Andrei Ivanov

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
