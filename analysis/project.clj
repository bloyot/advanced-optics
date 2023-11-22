(defproject analysis "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[cheshire "5.12.0"]
                 [clj-http "3.12.3"]
                 [clojure.java-time "1.4.2"]
                 [com.github.seancorfield/next.jdbc "1.3.894"]
                 [com.github.seancorfield/honeysql "2.5.1091"]
                 [org.clojure/clojure "1.11.1"]
                 [org.xerial/sqlite-jdbc "3.44.0.0"]]
  :repl-options {:init-ns analysis.stats})
