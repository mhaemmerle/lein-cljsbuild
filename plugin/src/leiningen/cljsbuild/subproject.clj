(ns leiningen.cljsbuild.subproject
  "Utilities for running cljsbuild in a subproject"
  (:require
    [clojure.string :as string]))

(def cljsbuild-version "0.3.3-SNAPSHOT")
(def required-clojure-version "1.5.1")

(def cljsbuild-dependencies
  [['cljsbuild cljsbuild-version]
   ['org.clojure/clojure required-clojure-version]])

(defn- numeric-version [v]
  (map #(Integer. %) (re-seq #"\d+" (first (string/split v #"-" 2)))))

(defn- version-satisfies? [v1 v2]
  (let [v1 (numeric-version v1)
        v2 (numeric-version v2)]
    (loop [versions (map vector v1 v2)
           [seg1 seg2] (first versions)]
      (cond (empty? versions) true
            (= seg1 seg2) (recur (rest versions) (first (rest versions)))
            (> seg1 seg2) true
            (< seg1 seg2) false))))

(defn check-clojure-version [project-dependencies]
  (if-let [clojure-dependency ('org.clojure/clojure project-dependencies)]
    (let [version (first clojure-dependency)]
      (when (not (version-satisfies? version required-clojure-version))
        (throw
          (Exception.
            (str "The ClojureScript compiler requires Clojure version >= " required-clojure-version)))))))

(defn dependency-map [dependency-vec]
  (into {} (map (juxt first rest) dependency-vec)))

(defn merge-dependencies [project-dependencies]
  (let [project (dependency-map project-dependencies)
        cljsbuild (dependency-map cljsbuild-dependencies)]
    (check-clojure-version project)
    (when-not ('org.clojure/clojurescript project)
      (println "\033[33mWARNING: It appears your project does not contain a ClojureScript"
               "dependency. One will be provided for you by lein-cljsbuild, but it"
               "is strongly recommended that you add your own.  You can find a list"
               "of all ClojureScript releases here:")
      (println "http://search.maven.org/#search|ga|1|g%3A%22org.clojure%22%20AND%20a%3A%22clojurescript%22\033[0m"))
    (map (fn [[k v]] (vec (cons k v)))
      (merge cljsbuild project))))

(defn make-subproject [project crossover-path builds]
  (with-meta
    {:local-repo-classpath true
     :dependencies (merge-dependencies (:dependencies project))
     :repositories (:repositories project)
     :source-paths (concat
                     (:source-paths project)
                     (mapcat :source-paths builds)
                     [crossover-path])
     :resource-paths (:resource-paths project)
     :checkout-deps-shares (:checkout-deps-shares project)
     :eval-in (:eval-in project)}
    (meta project)))
