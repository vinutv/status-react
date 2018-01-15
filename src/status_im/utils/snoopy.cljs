(ns status-im.utils.snoopy)

(comment
 (def snoopy (.-default (js/require "rn-snoopy")))

 (def bars (js/require "rn-snoopy/stream/bars"))
 (def sn-filter (.-default (js/require "rn-snoopy/stream/filter")))
 (def buffer (js/require "rn-snoopy/stream/buffer"))

 (def Emitter (js/require "react-native/Libraries/vendor/emitter/EventEmitter"))

 (def emitter (Emitter.))
 (def events (.stream snoopy emitter))


 (def f (sn-filter (clj->js {}), true)))

(defn subscribe []
  (let [snoopy (.-default (js/require "rn-snoopy"))
        sn-filter (.-default (js/require "rn-snoopy/stream/filter"))
        Emitter (js/require "react-native/Libraries/vendor/emitter/EventEmitter")
        emitter (Emitter.)
        events (.stream snoopy emitter)
        f (sn-filter (clj->js {}) true)]
    (.subscribe (f events))))

