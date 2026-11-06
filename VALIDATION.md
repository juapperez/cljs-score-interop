# Local validation

Executed on 6 November 2026 with Node v20.20.2 and the pinned nbb 1.6.214 dependency:

```text
Testing score-interop.core-test

Ran 10 tests containing 35 assertions.
0 failures, 0 errors.
```

Coverage includes Metro asset-key conversion, JavaScript object identity for refs/styles, preservation of false/zero callback arguments, structured score conversion, malformed input rejection, method receiver preservation, command ranges, missing handles and quoted alphaTex strings.

The tests call JavaScript objects and functions through nbb/SCI. The handle objects are synthetic fixtures. No ClojureScript compiler, React Native runtime, WebView, real score file or audio engine was executed. These results do not establish iOS/Android integration or production readiness. Upstream API references use expo-alphatab 0.1.0, commit `24ec2b868b150bbd2e89406386f55fed4b957eeb`.

Code, tests and documentation were created with AI assistance. Independent review remains proposed.
