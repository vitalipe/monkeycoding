(defproject monkeycoding "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.9.908"]
                 [reagent "0.8.0-alpha2"]
                 [cljsjs/codemirror "5.24.0-1"]]

  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.13"]
            [lein-npm "0.6.2"]]

  :npm {
        ;; we use node for node-sass because it's faster and less buggy
        ;; and we also use it to run the player app build for the embedded player
        :devDependencies [[node-sass "4.5.3"]]
        :package {:scripts
                  {:build-app-sass "node-sass style/app.scss public/css/app.css --output-style compressed"
                   :build-app-player "cd ./playback && npm run build-app-player"

                   ;; build in debug & watch for changes
                   :watch-app-player "cd ./playback && npm run watch-app-player"
                   :watch-app-sass   "node-sass style/app.scss public/css/app.css && node-sass style/app.scss public/css/app.css --watch"}}}


  :min-lein-version "2.5.0"

  :clean-targets ^{:protect false}
  [:target-path
   [:cljsbuild :builds :app :compiler :output-dir]
   [:cljsbuild :builds :app :compiler :output-to]]

  :resource-paths ["public"]

  :figwheel {:http-server-root "."
             :nrepl-port 7002
             :nrepl-middleware ["cemerick.piggieback/wrap-cljs-repl"]
             :css-dirs ["public/css"]}

  :cljsbuild {:builds {:app
                       {:source-paths ["src" "env/dev/cljs"]
                        :compiler
                        {:main "monkeycoding.dev"
                         :output-to "public/js/app.js"
                         :output-dir "public/js/out"
                         :asset-path   "js/out"
                         :source-map true
                         :optimizations :none
                         :pretty-print  true}
                        :figwheel
                        {:on-jsload "monkeycoding.core/mount-root"
                         :open-urls ["http://localhost:3449/index.html"]}}
                       :release
                       {:source-paths ["src" "env/prod/cljs"]
                        :compiler
                        {:output-to "public/js/app.js"
                         :output-dir "public/js/release"
                         :asset-path   "js/out"
                         :optimizations :advanced
                         :pretty-print false}}}}

  :aliases {
            "package-app" ["do"
                            "clean"
                            ["npm" "run" "build-app-sass"]
                            ["npm" "run" "build-app-player"]
                            ["cljsbuild" "once" "release"]]

            "init" ["do"
                    ["npm" "install"]]

            "dev" ["pdo"
                    ["npm" "run" "watch-app-sass"]
                    ["npm" "run" "watch-app-player"]
                    "figwheel"]}


  :profiles {:dev {:dependencies [[binaryage/devtools "0.9.4"]
                                  [figwheel-sidecar "0.5.13"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [com.cemerick/piggieback "0.2.2"]]}})
