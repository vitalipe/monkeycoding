(ns monkeycoding.editor.codemirror
    (:require
      [cljsjs.codemirror]

      ;; plugins
      [cljsjs.codemirror.addon.scroll.simplescrollbars]
      [cljsjs.codemirror.addon.mode.simple]

      ;; languages
      [cljsjs.codemirror.mode.apl]
      [cljsjs.codemirror.mode.asn.1]
      [cljsjs.codemirror.mode.brainfuck]
      [cljsjs.codemirror.mode.clike]
      [cljsjs.codemirror.mode.clojure]
      [cljsjs.codemirror.mode.cmake]
      [cljsjs.codemirror.mode.cobol]
      [cljsjs.codemirror.mode.coffeescript]
      [cljsjs.codemirror.mode.commonlisp]
      [cljsjs.codemirror.mode.css]
      [cljsjs.codemirror.mode.d]
      [cljsjs.codemirror.mode.dart]
      [cljsjs.codemirror.mode.dockerfile]
      [cljsjs.codemirror.mode.dylan]
      [cljsjs.codemirror.mode.eiffel]
      [cljsjs.codemirror.mode.elm]
      [cljsjs.codemirror.mode.erlang]
      [cljsjs.codemirror.mode.factor]
      [cljsjs.codemirror.mode.forth]
      [cljsjs.codemirror.mode.fortran]
      [cljsjs.codemirror.mode.gas]
      [cljsjs.codemirror.mode.go]
      [cljsjs.codemirror.mode.groovy]
      [cljsjs.codemirror.mode.haml]
      [cljsjs.codemirror.mode.handlebars]
      [cljsjs.codemirror.mode.haskell]
      [cljsjs.codemirror.mode.haxe]
      [cljsjs.codemirror.mode.htmlembedded]
      [cljsjs.codemirror.mode.htmlmixed]
      [cljsjs.codemirror.mode.idl]
      [cljsjs.codemirror.mode.javascript]
      [cljsjs.codemirror.mode.julia]
      [cljsjs.codemirror.mode.livescript]
      [cljsjs.codemirror.mode.lua]
      [cljsjs.codemirror.mode.markdown]
      [cljsjs.codemirror.mode.mathematica]
      [cljsjs.codemirror.mode.mllike]
      [cljsjs.codemirror.mode.modelica]
      [cljsjs.codemirror.mode.mscgen]
      [cljsjs.codemirror.mode.mumps]
      [cljsjs.codemirror.mode.nginx]
      [cljsjs.codemirror.mode.ntriples]
      [cljsjs.codemirror.mode.octave]
      [cljsjs.codemirror.mode.oz]
      [cljsjs.codemirror.mode.pascal]
      [cljsjs.codemirror.mode.pegjs]
      [cljsjs.codemirror.mode.perl]
      [cljsjs.codemirror.mode.php]
      [cljsjs.codemirror.mode.pig]
      [cljsjs.codemirror.mode.properties]
      [cljsjs.codemirror.mode.puppet]
      [cljsjs.codemirror.mode.python]
      [cljsjs.codemirror.mode.q]
      [cljsjs.codemirror.mode.r]
      [cljsjs.codemirror.mode.rpm]
      [cljsjs.codemirror.mode.rst]
      [cljsjs.codemirror.mode.ruby]
      [cljsjs.codemirror.mode.rust]
      [cljsjs.codemirror.mode.sass]
      [cljsjs.codemirror.mode.scheme]
      [cljsjs.codemirror.mode.shell]
      [cljsjs.codemirror.mode.sieve]
      [cljsjs.codemirror.mode.slim]
      [cljsjs.codemirror.mode.smalltalk]
      [cljsjs.codemirror.mode.smarty]
      [cljsjs.codemirror.mode.solr]
      [cljsjs.codemirror.mode.soy]
      [cljsjs.codemirror.mode.sparql]
      [cljsjs.codemirror.mode.spreadsheet]
      [cljsjs.codemirror.mode.sql]
      [cljsjs.codemirror.mode.stex]
      [cljsjs.codemirror.mode.stylus]
      [cljsjs.codemirror.mode.swift]
      [cljsjs.codemirror.mode.tcl]
      [cljsjs.codemirror.mode.textile]
      [cljsjs.codemirror.mode.tiddlywiki]
      [cljsjs.codemirror.mode.tiki]
      [cljsjs.codemirror.mode.toml]
      [cljsjs.codemirror.mode.tornado]
      [cljsjs.codemirror.mode.troff]
      [cljsjs.codemirror.mode.ttcn-cfg]
      [cljsjs.codemirror.mode.ttcn]
      [cljsjs.codemirror.mode.turtle]
      [cljsjs.codemirror.mode.twig]
      [cljsjs.codemirror.mode.vb]
      [cljsjs.codemirror.mode.vbscript]
      [cljsjs.codemirror.mode.velocity]
      [cljsjs.codemirror.mode.verilog]
      [cljsjs.codemirror.mode.vhdl]
      [cljsjs.codemirror.mode.vue]
      [cljsjs.codemirror.mode.xml]
      [cljsjs.codemirror.mode.yaml]
      [cljsjs.codemirror.mode.z80]))


(def languages {
                :apl {:value "apl" :display-name "APL"}
                :asn.1 {:value "asn.1" :display-name "ASN.1"}
                :brainfuck {:value "brainfuck" :display-name "Brainfuck"}
                :clike {:value "clike" :display-name "C/C++/C#/Objective-C"}
                :clojure {:value "clojure" :display-name "Clojure"}
                :cmake {:value "cmake" :display-name "CMake"}
                :cobol {:value "cobol" :display-name "COBOL"}
                :coffeescript {:value "coffeescript" :display-name "CoffeeScript"}
                :commonlisp {:value "commonlisp" :display-name "Common LISP"}
                :css {:value "css" :display-name "CSS"}
                :d {:value "d" :display-name "D"}
                :dart {:value "dart" :display-name "Dart"}
                :dockerfile {:value "dockerfile" :display-name "Dockerfile"}
                :dylan {:value "dylan" :display-name "Dylan"}
                :eiffel {:value "eiffel" :display-name "Eiffel"}
                :elm {:value "elm" :display-name "Elm"}
                :erlang {:value "erlang" :display-name "Erlang"}
                :factor {:value "factor" :display-name "Factor"}
                :forth {:value "forth" :display-name "Forth"}
                :fortran {:value "fortran" :display-name "Fortran"}
                :gas {:value "gas" :display-name "AT&T assembly (GAS)"}
                :go {:value "go" :display-name "Go"}
                :groovy {:value "groovy" :display-name "Groovy"}
                :haml {:value "haml" :display-name "HAML"}
                :handlebars {:value "handlebars" :display-name "Handlebars"}
                :haskell {:value "haskell" :display-name "Haskell"}
                :haxe {:value "haxe" :display-name "Haxe"}
                :htmlembedded {:value "htmlembedded" :display-name "HTML (embedded)"}
                :htmlmixed {:value "htmlmixed" :display-name "HTML (mixed mode)"}
                :idl {:value "idl" :display-name "IDL"}
                :javascript {:value "javascript" :display-name "JavaScript"}
                :julia {:value "julia" :display-name "Julia"}
                :livescript {:value "livescript" :display-name "LiveScript"}
                :lua {:value "lua" :display-name "Lua"}
                :markdown {:value "markdown" :display-name "Markdown (github)"}
                :mathematica {:value "mathematica" :display-name "Mathematica"}
                :mllike {:value "mllike" :display-name "OCaml/F# (ML like)"}
                :nginx {:value "nginx" :display-name "NGINX config"}
                :octave {:value "octave" :display-name "Octave"}
                :oz {:value "oz" :display-name "Oz"}
                :pascal {:value "pascal" :display-name "Pascal"}
                :perl {:value "perl" :display-name "Perl"}
                :php {:value "php" :display-name "PHP"}
                :puppet {:value "puppet" :display-name "Puppet"}
                :r {:value "r" :display-name "R"}
                :ruby {:value "ruby" :display-name "Ruby"}
                :rust {:value "rust" :display-name "Rust"}
                :sass {:value "sass" :display-name "SASS"}
                :scheme {:value "scheme" :display-name "Scheme"}
                :shell {:value "shell" :display-name "Shell (bash/sh)"}
                :sieve {:value "sieve" :display-name "Sieve"}
                :slim {:value "slim" :display-name "SLIM"}
                :smalltalk {:value "smalltalk" :display-name "Smalltalk"}
                :sql {:value "sql" :display-name "SQL"}
                :stex {:value "stex" :display-name "sTeX"}
                :stylus {:value "stylus" :display-name "Stylus"}
                :swift {:value "swift" :display-name "Swift"}
                :tcl {:value "tcl" :display-name "TCL"}
                :vb {:value "vb" :display-name "VB.NET"}
                :vbscript {:value "vbscript" :display-name "VBScript"}
                :verilog {:value "verilog" :display-name "Verilog"}
                :vhdl {:value "vhdl" :display-name "VHDL"}
                :vue {:value "vue" :display-name "Vue.js Templates"}
                :xml {:value "xml" :display-name "XML"}
                :yaml {:value "yaml" :display-name "YAML"}
                :z80 {:value "z80" :display-name "Z80 Assembly"}})













;; config options that override everything})
(def hardcoded-options {
                          "scrollbarStyle" "overlay"
                          "coverGutterNextToScrollbar" true})


(defn config->cm-options [config]
  {
    "lineNumbers" (:show-line-numbers config)
    "theme" (:theme config)
    "mode" (:language config)
    "readOnly" (:read-only config)})


(defn- create-codemirror! [dom-node config]
  (let [options (clj->js (merge (config->cm-options config) hardcoded-options))]
    (new  js/CodeMirror dom-node options)))
