(ns score-interop.core-test
  (:require [cljs.test :refer [deftest is testing]]
            [score-interop.core :as interop]))

(def assets {:engine 1 :font 2 :sound-font 3})

(deftest metro-assets-and-js-identity
  (let [style #js {:flex 1} ref #js {:current nil}
        p (interop/props {:assets assets :style style :ref ref
                          :theme {:layoutMode "horizontal" :staffLine "#333"}})]
    (is (= 3 (.. p -assets -soundFont)))
    (is (identical? style (.-style p)))
    (is (identical? ref (.-ref p)))
    (is (= "horizontal" (.. p -theme -layoutMode)))
    (is (not (js/Object.hasOwn p "onReady")))))

(deftest callback-arguments-preserve-false-and-zero
  (let [events (atom [])
        p (interop/props {:assets assets :on-state #(swap! events conj %)
                          :on-position #(swap! events conj [%1 %2])})]
    (.onState p false)
    (.onPosition p 0 5000)
    (is (= [false [0 5000]] @events))))

(deftest score-js-to-clojure-boundary
  (let [seen (atom nil) p (interop/props {:assets assets :on-score #(reset! seen %)})]
    (.onScore p #js {:title "Canción" :artist "" :tracks #js [#js {:index 0 :name "Guitarra" :program 24}]})
    (is (= {:title "Canción" :artist "" :tracks [{:index 0 :name "Guitarra" :program 24}]} @seen))))

(deftest rejects-incomplete-or-wrong-score-data
  (doseq [s [nil #js {} #js {:title "x" :artist "" :tracks nil}
             #js {:title "x" :artist "" :tracks #js [#js {:index -1 :name "x" :program 0}]}]]
    (is (thrown? js/Error (interop/score->data s)))))

(deftest rejects-bad-props-before-js-boundary
  (doseq [options [{:assets {:engine "1" :font 2 :sound-font 3}}
                   {:assets assets :on-ready false}
                   {:assets assets :onReady (fn [])}
                   {:assets assets :theme "dark"}
                   {:assets (assoc assets :extra 4)}]]
    (is (thrown? js/Error (interop/props options)))))

(deftest imperative-arguments-and-method-receiver
  (let [calls (atom []) handle #js {}]
    (aset handle "setMute" (fn [track on] (this-as self (swap! calls conj [self track on]))))
    (interop/invoke! handle :mute 0 false)
    (is (identical? handle (ffirst @calls)))
    (is (= [0 false] (vec (rest (first @calls)))))))

(deftest numeric-boundaries-forward-valid-zero
  (let [values (atom []) handle #js {:setVolume #(swap! values conj %) :seek #(swap! values conj %)}]
    (interop/invoke! handle :volume 0)
    (interop/invoke! handle :seek 0)
    (is (= [0 0] @values))))

(deftest invalid-commands-never-call-handle
  (let [calls (atom 0) handle #js {:setSpeed #(swap! calls inc) :setVolume #(swap! calls inc)
                                  :seek #(swap! calls inc) :setMute #(swap! calls inc)}]
    (doseq [[op & args] [[:speed js/NaN] [:speed js/Infinity] [:speed 0] [:speed 3]
                         [:volume -1] [:volume 2] [:seek 0.5] [:seek -1]
                         [:mute 0 "false"] [:mute -1 true] [:unknown]]]
      (is (thrown? js/Error (apply interop/invoke! handle op args))))
    (is (zero? @calls))))

(deftest missing-handle-and-arity-errors
  (is (thrown? js/Error (interop/invoke! nil :stop)))
  (is (thrown? js/Error (interop/invoke! #js {} :play-pause)))
  (is (thrown? js/Error (interop/invoke! #js {:stop (fn [])} :stop 1))))

(deftest load-tex-and-transport
  (let [events (atom []) handle #js {:loadTex #(swap! events conj %) :stop #(swap! events conj :stop)}
        text "\\title \"Quoted song\"\n.\n3.3"]
    (interop/invoke! handle :load-tex text)
    (interop/invoke! handle :stop)
    (is (= [text :stop] @events))))
