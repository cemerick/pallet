(ns pallet.crate.hudson-test
  (:use [pallet.crate.hudson] :reload-all)
  (:require
   [pallet.template :only [apply-templates]]
   [pallet.core :as core]
   [pallet.target :as target]
   [pallet.stevedore :as stevedore]
   [pallet.resource.remote-file :as remote-file]
   [pallet.resource.directory :as directory]
   [pallet.resource.user :as user]
   [pallet.crate.maven :as maven]
   [pallet.utils :as utils]
   [net.cgrand.enlive-html :as xml])
  (:use clojure.test
        pallet.test-utils
        [pallet.resource.package :only [package package-manager]]
        [pallet.resource :only [build-resources]]
        [pallet.stevedore :only [script]]))

(use-fixtures :each with-null-target)

(deftest hudson-tomcat-test
  (is (= "echo \"directory /var/lib/hudson...\"\n{ mkdir -p /var/lib/hudson && chown  root /var/lib/hudson && chgrp  tomcat6 /var/lib/hudson && chmod  775 /var/lib/hudson; } || { echo directory /var/lib/hudson failed ; exit 1 ; } >&2 \necho \"...done\"\necho \"remote-file /var/lib/hudson/hudson.war...\"\n{ if [ \\( ! -e /var/lib/hudson/hudson.war -o \\( \"5d616c367d7a7888100ae6e98a5f2bd7\" != \"$(md5sum /var/lib/hudson/hudson.war | cut -f1 -d' ')\" \\) \\) ]; then wget -O /var/lib/hudson/hudson.war http://hudson-ci.org/latest/hudson.war;fi && echo MD5 sum is $(md5sum /var/lib/hudson/hudson.war); } || { echo remote-file /var/lib/hudson/hudson.war failed ; exit 1 ; } >&2 \necho \"...done\"\necho \"remote-file /etc/tomcat6/policy.d/99hudson.policy...\"\n{ { cat > /etc/tomcat6/policy.d/99hudson.policy <<'EOF'\ngrant codeBase \"file:${catalina.base}/webapps/hudson/-\" {\n  permission java.security.AllPermission;\n};\ngrant codeBase \"file:/var/lib/hudson/-\" {\n  permission java.security.AllPermission;\n};\nEOF\n }; } || { echo remote-file /etc/tomcat6/policy.d/99hudson.policy failed ; exit 1 ; } >&2 \necho \"...done\"\necho \"remote-file /etc/tomcat6/Catalina/localhost/hudson.xml...\"\n{ { cat > /etc/tomcat6/Catalina/localhost/hudson.xml <<'EOF'\n<?xml version=\"1.0\" encoding=\"utf-8\"?>\n <Context\n privileged=\"true\"\n path=\"/hudson\"\n allowLinking=\"true\"\n swallowOutput=\"true\"\n >\n <Environment\n name=\"HUDSON_HOME\"\n value=\"/var/lib/hudson\"\n type=\"java.lang.String\"\n override=\"false\"/>\n </Context>\nEOF\n }; } || { echo remote-file /etc/tomcat6/Catalina/localhost/hudson.xml failed ; exit 1 ; } >&2 \necho \"...done\"\necho \"remote-file /var/lib/tomcat6/webapps/hudson.war...\"\n{ cp /var/lib/hudson/hudson.war /var/lib/tomcat6/webapps/hudson.war && chown  tomcat6 /var/lib/tomcat6/webapps/hudson.war && chgrp  tomcat6 /var/lib/tomcat6/webapps/hudson.war && chmod  600 /var/lib/tomcat6/webapps/hudson.war; } || { echo remote-file /var/lib/tomcat6/webapps/hudson.war failed ; exit 1 ; } >&2 \necho \"...done\"\n"
         (test-resource-build
          [nil {} :tomcat {:user "tomcat6" :group "tomcat6"}]
          (tomcat-deploy)
          (parameters-test
           [:hudson :user] "tomcat6"
           [:hudson :group] "tomcat6")))))

(deftest determine-scm-type-test
  (is (= :git (determine-scm-type ["http://project.org/project.git"]))))

(deftest normalise-scms-test
  (is (= [["http://project.org/project.git"]]
         (normalise-scms ["http://project.org/project.git"]))))

(deftest output-scm-for-git-test
  (is (= "<org.spearce.jgit.transport.RemoteConfig>\n  <string>origin</string>\n  <int>5</int>\n  <string>fetch</string>\n  <string>+refs/heads/*:refs/remotes/origin/*</string>\n  <string>receivepack</string>\n  <string>git-upload-pack</string>\n  <string>uploadpack</string>\n  <string>git-upload-pack</string>\n  <string>url</string>\n  <string>http://project.org/project.git</string>\n  <string>tagopt</string>\n  <string></string>\n</org.spearce.jgit.transport.RemoteConfig>"
         (apply str
          (xml/emit*
           (output-scm-for :git {:tag :b :image [:ubuntu]}
                           "http://project.org/project.git" {}))))))

(deftest hudson-job-test
  (core/defnode n [])
  (is (= (stevedore/do-script
          (directory/directory* "/var/lib/hudson/jobs/project" :p true)
          (remote-file/remote-file*
           "/var/lib/hudson/jobs/project/config.xml"
           :content "<?xml version='1.0' encoding='utf-8'?>\n<maven2-moduleset>\n  <actions></actions>\n  <description></description>\n  <logRotator>\n    <daysToKeep>-1</daysToKeep>\n    <numToKeep>-1</numToKeep>\n    <artifactDaysToKeep>-1</artifactDaysToKeep>\n    <artifactNumToKeep>-1</artifactNumToKeep>\n  </logRotator>\n  <keepDependencies>false</keepDependencies>\n  <properties></properties>\n  <scm class=\"hudson.plugins.git.GitSCM\">\n    <remoteRepositories><org.spearce.jgit.transport.RemoteConfig>\n  <string>origin</string>\n  <int>5</int>\n  <string>fetch</string>\n  <string>+refs/heads/*:refs/remotes/origin/*</string>\n  <string>receivepack</string>\n  <string>git-upload-pack</string>\n  <string>uploadpack</string>\n  <string>git-upload-pack</string>\n  <string>url</string>\n  <string>http://project.org/project.git</string>\n  <string>tagopt</string>\n  <string></string>\n</org.spearce.jgit.transport.RemoteConfig>\n      \n    </remoteRepositories>\n    <branches>\n      <hudson.plugins.git.BranchSpec>\n        <name>origin/master</name>\n      </hudson.plugins.git.BranchSpec>\n    </branches>\n    <mergeOptions></mergeOptions>\n    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>\n    <submoduleCfg class=\"list\"></submoduleCfg>\n  </scm>\n  <canRoam>true</canRoam>\n  <disabled>false</disabled>\n  <blockBuildWhenUpstreamBuilding>false</blockBuildWhenUpstreamBuilding>\n  <triggers class=\"vector\">\n    <hudson.triggers.SCMTrigger>\n      <spec>*/15 * * * *</spec>\n    </hudson.triggers.SCMTrigger>\n  </triggers>\n  <concurrentBuild>false</concurrentBuild>\n  <rootModule>\n    <groupId>project</groupId>\n    <artifactId>artifact</artifactId>\n  </rootModule>\n  <goals>clojure:test</goals>\n  <mavenOpts>-Dx=y</mavenOpts>\n  <mavenName>base maven</mavenName>\n  <aggregatorStyleBuild>false</aggregatorStyleBuild>\n  <incrementalBuild>false</incrementalBuild>\n  <usePrivateRepository>false</usePrivateRepository>\n  <ignoreUpstremChanges>false</ignoreUpstremChanges>\n  <archivingDisabled>false</archivingDisabled>\n  <reporters></reporters>\n  <publishers></publishers>\n  <buildWrappers></buildWrappers>\n</maven2-moduleset>")
          (directory/directory*
           "/var/lib/hudson"
           :owner "root" :group "tomcat6"
           :mode "g+w"
           :recursive true))
         (test-resource-build
          [nil {:image [:ubuntu]} :hudson {:user "tomcat6" :group "tomcat6"}]
          (job
           :maven2 "project"
           :maven-opts "-Dx=y"
           :scm ["http://project.org/project.git"])))))


(deftest hudson-maven-xml-test
  (core/defnode test-node [:ubuntu])
  (is (= "<?xml version='1.0' encoding='utf-8'?>\n<hudson.tasks.Maven_-DescriptorImpl>\n  <installations>\n    <hudson.tasks.Maven_-MavenInstallation>\n      <name>name</name>\n      <home></home>\n      <properties>\n        <hudson.tools.InstallSourceProperty>\n          <installers>\n            <hudson.tasks.Maven_-MavenInstaller>\n              <id>2.2.0</id>\n            </hudson.tasks.Maven_-MavenInstaller>\n          </installers>\n        </hudson.tools.InstallSourceProperty>\n      </properties>\n    </hudson.tasks.Maven_-MavenInstallation>\n  </installations>\n</hudson.tasks.Maven_-DescriptorImpl>"
         (apply str (hudson-maven-xml
                      test-node
                      [["name" "2.2.0"]])))))

(deftest hudson-maven-test
  (is (= (stevedore/do-script
          (directory/directory*
           "/usr/share/tomcat6/.m2" :group "tomcat6" :mode "g+w")
          (directory/directory*
           "/var/lib/hudson"
           :owner "root" :group "tomcat6" :mode "775")
          (remote-file/remote-file*
           "/var/lib/hudson/hudson.tasks.Maven.xml"
           :content "<?xml version='1.0' encoding='utf-8'?>\n<hudson.tasks.Maven_-DescriptorImpl>\n  <installations>\n    <hudson.tasks.Maven_-MavenInstallation>\n      <name>default maven</name>\n      <home></home>\n      <properties>\n        <hudson.tools.InstallSourceProperty>\n          <installers>\n            <hudson.tasks.Maven_-MavenInstaller>\n              <id>2.2.0</id>\n            </hudson.tasks.Maven_-MavenInstaller>\n          </installers>\n        </hudson.tools.InstallSourceProperty>\n      </properties>\n    </hudson.tasks.Maven_-MavenInstallation>\n  </installations>\n</hudson.tasks.Maven_-DescriptorImpl>"
           :owner "root"
           :group "tomcat6")
          (maven/download*
           :maven-home "/var/lib/hudson/tools/default_maven"
           :version "2.2.0"
           :owner "root"
           :group "tomcat6"))
         (test-resource-build
          [nil {:image [:ubuntu]} :hudson {:user "tomcat6" :group "tomcat6"}]
          (maven "default maven" "2.2.0")))))

(deftest plugin-test
  (is (= (stevedore/do-script
          (directory/directory* "/var/lib/hudson/plugins")
          (remote-file/remote-file*
           "/var/lib/hudson/plugins/git.hpi"
           :md5 "98db63b28bdf9ab0e475c2ec5ba209f1"
           :url "https://hudson.dev.java.net/files/documents/2402/135478/git.hpi")
          (user/user* "tomcat6" :action :manage :comment "hudson"))
         (test-resource-build
          [nil {} :hudson {:user "tomcat6" :group "tomcat6"}]
          (plugin :git)))))
