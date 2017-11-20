(ns ^:figwheel-no-load multi-client-ws.app
  (:require [multi-client-ws.core :as core]
            [devtools.core :as devtools]))

(enable-console-print!)

(devtools/install!)

(core/init!)
