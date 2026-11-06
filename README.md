# cljs-score-interop

An experimental ClojureScript boundary adapter for [expo-alphatab](https://github.com/juapperez/expo-alphatab) 0.1.0. It converts Clojure maps into the component's JavaScript props, turns `onScore` data back into Clojure data, and checks imperative command arguments before calling a score handle.

This is a small prototype written on 6 November 2026 with AI assistance. There is no established Clojure community adoption. The underlying Expo package has a separate iOS integration history; that does **not** validate this new adapter on mobile.

## Run the boundary tests

Node 20 was used. Install the pinned development dependency, then run:

```sh
npm ci
npm test
```

The suite uses nbb 1.6.214, which interprets a subset of ClojureScript through SCI on Node. Tests exercise real JavaScript objects and callbacks. They do not compile with the ClojureScript compiler, load React Native, mount a WebView, render a score, or play audio. Native integration, Android behavior and advanced compiler compatibility remain unverified.

## API

```clojure
(require '[score-interop.core :as score])

;; These numbers stand in for actual Metro require() asset IDs.
(def p (score/props
         {:assets {:engine 1 :font 2 :sound-font 3}
          :on-score (fn [data] (println (:tracks data)))
          :on-position (fn [current-ms end-ms] (println current-ms end-ms))}))

;; In a future native integration, pass p to React.createElement with
;; AlphaTabScore. After mount, invoke a method on your actual ref.current:
;; (score/invoke! handle :volume 0.5)
;; (score/invoke! handle :mute 0 false)
```

Props use kebab-case callback keys, while `:theme` uses upstream keys such as `:staffLine` and `:layoutMode`. `:style` and `:ref` pass through as JavaScript values. Missing handles throw instead of silently discarding commands. The asset IDs in this example are placeholders, not bundled assets.

## Limits and proposed next work

This is not a complete player, a published npm/Clojars package or a security boundary. It does not validate encoded music files, model WebView lifecycle, or provide every upstream validation rule. A useful next stage would compile the adapter, mount it in a minimal Expo app, verify boot/ready/load/unmount behavior on iOS, investigate Android, and publish English/Spanish setup instructions and a compatibility matrix. These are proposed deliverables, not completed work.

## Sources and licensing

API contract reviewed against expo-alphatab commit `24ec2b868b150bbd2e89406386f55fed4b957eeb`, especially `src/AlphaTabScore.tsx`, `src/bootstrap.ts` and `src/protocol.ts`. This repository contains only newly written adapter code, tests and documentation, licensed MIT. No commercial app source, music recordings, bundled soundfonts or personal application records are included. Upstream dependencies keep their own licenses: expo-alphatab MIT, nbb EPL-1.0, alphaTab MPL-2.0, Bravura OFL-1.1 and sonivox Apache-2.0.
