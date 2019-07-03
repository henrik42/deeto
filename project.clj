;; re-enables http repository support in Leiningen 2.8
(require 'cemerick.pomegranate.aether)
(cemerick.pomegranate.aether/register-wagon-factory!
 "http" #(org.apache.maven.wagon.providers.http.HttpWagon.))

(defproject deeto "0.1.1-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :java-source-paths ["java/test" "java/src"]
  :target-path "target/%s"
  :aot [deeto.java-api
        deeto.core]
  :dependencies [[org.clojure/clojure "1.8.0"]]
  :plugins [[lein-swank "1.4.5"]]
    
  :aliases {"deploy" ["do" "clean," "deploy"]}
            
  :release-tasks [["test"]
                  ["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  #_ ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  #_ ["deploy"]
                  #_ ["vcs" "push"]]

  :profiles {;; run a local Nexus in a docker container with:
             ;; docker run -d -p 8081:8081 --name nexus sonatype/nexus:oss
             ;;
             ;; Then you can deploy a SNAPSHOT or release via
             ;; lein with-profile +local deploy
             :local {:repositories [["snapshots" {:url "http://localhost:8081/nexus/content/repositories/snapshots/"
                                                  :sign-releases false :username "admin" :password "admin123"}]
                                    ["releases" {:url "http://localhost:8081/nexus/content/repositories/releases/"
                                                 :sign-releases false :username "admin" :password "admin123"}]]}})

            

