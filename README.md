# day-of-datomic-cloud

Day of Datomic Cloud project is a collection of samples and tutorials
for learning [Datomic Cloud](http://datomic.com) at a Clojure REPL.

## Getting Started

* Install Clojure:
```
$ brew install clojure
```

* [Create a Datomic Cloud System](https://docs.datomic.com/cloud/getting-started/getting-started.html).

* Clone this repo locally

* Make a copy of the config.edn.example file and edit it, providing your Datomic Cloud connection [configuration information](https://docs.datomic.com/cloud/getting-started/connecting.html#creating-database).

* Make sure you are running a [SOCKS Proxy](https://docs.datomic.com/cloud/getting-started/configuring-access.html#socks-proxy) to your Datomic Cloud Bastion.

* Start a Clojure REPL in the day-of-datomic-cloud directory:
```
$ clj
```

Work through some of the tutorials in the tutorial directory,
evaluating each form at the REPL. You might start with:

* hello_world.clj
* social_news.clj
* provenance.clj
* graph.clj
* filters.clj

## Study the Samples

As or after you work through the tutorial, you may want to also study
the helper functions in src/datomic/samples.

## Questions, Feedback?

For specific feedback on the tutorials, please create an
[issue](https://github.com/cognitect-labs/day-of-datomic-cloud/issues).

For questions about Datomic, try the [public discussion forum](http://forum.datomic.com/).

## License

Copyright © 2017 Cognitect, Inc.

Licensed under the Eclipse Public License (EPL) v1.0
