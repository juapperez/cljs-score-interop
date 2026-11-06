(ns score-interop.core)

;; Original experimental adapter, authored with AI assistance.
;; Contract: expo-alphatab 0.1.0. Does not load React Native or a WebView.
(defn- ensure! [condition message]
  (when-not condition (throw (js/Error. message))))

(defn- finite? [x] (and (number? x) (js/Number.isFinite x)))
(defn- integer>=0? [x] (and (finite? x) (js/Number.isInteger x) (<= 0 x)))

(defn score->data
  "Convert the documented JS onScore payload to explicit Clojure data."
  [score]
  (ensure! (and score (string? (.-title score)) (string? (.-artist score))
                (js/Array.isArray (.-tracks score))) "Invalid score payload")
  {:title (.-title score)
   :artist (.-artist score)
   :tracks (mapv (fn [track]
                   (ensure! (and track (integer>=0? (.-index track))
                                 (string? (.-name track))
                                 (integer>=0? (.-program track))) "Invalid track payload")
                   {:index (.-index track) :name (.-name track) :program (.-program track)})
                 (array-seq (.-tracks score)))})

(def callbacks
  {:on-boot "onBoot" :on-ready "onReady" :on-score "onScore"
   :on-state "onState" :on-position "onPosition"
   :on-sound-font-progress "onSoundFontProgress" :on-error "onError"})

(defn props
  "Build JS props; style/ref remain JS values. Theme keys follow the upstream API.
   on-score receives a Clojure map. Other callbacks preserve upstream arguments."
  [{:keys [assets theme style ref] :as options}]
  (ensure! (map? options) "Options must be a map")
  (ensure! (every? (into #{:assets :theme :style :ref} (keys callbacks)) (keys options))
           "Unknown option: use documented kebab-case callback keys")
  (ensure! (and (map? assets)
                (= #{:engine :font :sound-font} (set (keys assets)))
                (every? integer>=0? (vals assets))) "Assets must be numeric Metro module IDs")
  (ensure! (or (nil? theme) (map? theme)) "Theme must be a map")
  (let [out #js {:assets #js {:engine (:engine assets) :font (:font assets)
                             :soundFont (:sound-font assets)}}]
    (when theme (aset out "theme" (clj->js theme)))
    (when (contains? options :style) (aset out "style" style))
    (when (contains? options :ref) (aset out "ref" ref))
    (doseq [[key js-key] callbacks :when (contains? options key)]
      (let [handler (get options key)]
        (ensure! (fn? handler) "Callback must be a function")
        (aset out js-key (if (= key :on-score)
                          (fn [score] (handler (score->data score)))
                          handler))))
    out))

(def methods
  {:load "load" :load-tex "loadTex" :play-pause "playPause" :stop "stop"
   :speed "setSpeed" :loop "setLoop" :metronome "setMetronome"
   :count-in "setCountIn" :mute "setMute" :solo "setSolo"
   :volume "setVolume" :zoom "setZoom" :seek "seek"})

(defn- valid-args? [op args]
  (case op
    (:play-pause :stop) (empty? args)
    (:load :load-tex) (and (= 1 (count args)) (string? (first args)) (seq (first args)))
    (:loop :metronome :count-in) (and (= 1 (count args)) (boolean? (first args)))
    (:mute :solo) (and (= 2 (count args)) (integer>=0? (first args)) (boolean? (second args)))
    :speed (and (= 1 (count args)) (finite? (first args)) (<= 0.25 (first args) 2))
    :volume (and (= 1 (count args)) (finite? (first args)) (<= 0 (first args) 1))
    :zoom (and (= 1 (count args)) (finite? (first args)) (<= 0.5 (first args) 2))
    :seek (and (= 1 (count args)) (integer>=0? (first args)))
    false))

(defn invoke!
  "Invoke an allowlisted AlphaTabScoreHandle method with checked arguments.
   Supply the actual handle (ref.current), not the ref object."
  [handle op & args]
  (let [method (get methods op)]
    (ensure! method "Unknown operation")
    (ensure! (valid-args? op args) "Invalid operation arguments")
    (ensure! (and handle (fn? (aget handle method))) "Score handle is not ready")
    (.apply (aget handle method) handle (to-array args))))
