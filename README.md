maven-jsign-plugin - Sign executables with Microsoft Authenticode
=================================================================

[![LICENSE](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)

The _maven-jsign-plugin_ provides a Maven goal to [JSign](https://github.com/arxes-tolina/jsign)

_maven-jsign-plugin_ is free to use and licensed under the Apache License version 2.0.

Build
=====

Since there's no JSign in Maven Central yet, clone or download [JSign](https://github.com/arxes-tolina/jsign) and build it with
 
`mvn clean install`

If you are behind a proxy, some tests will fail, so assumning your system has a system-wide proxy set, build with 

`mvn clean install -Djava.net.useSystemProxies=true`

You can also explicitly provide proxy settings by `-Dhttp.proxyHost=..., -Dhttp.proxyPort=...`

Once installed, you can build _maven-jsign-plugin_ with 

`mvn clean install`

Usage
=====

From commandline use 

`mvn de.tolina.maven.plugins:jsign-maven-plugin:signexe -Dkeystore=http://my.keystore.local/keystore -Dfile="D:\Temp\my.exe" -Dalias=mykey -Dstorepass=mystorepass`

or configure your POM

    <build>
      <plugins>
        <plugin>
          <groupId>de.tolina.maven.plugins</groupId>
            <artifactId>jsign-maven-plugin</artifactId>
            <version>1.0.0</version>
            <executions>
              <execution>
                <phase>package</phase>
                <goals>
                  <goal>signexe</goal>
                </goals>
                <configuration>
                  <alias>mykey</alias>
                  <file>D:\Temp\my.exe</file>
                  <keystore>http://my.keystore.local/keystore</keystore>
                  <storepass>mystorepass</storepass>
                </configuration>
              </execution>
            </executions>
         </plugin>
      </plugins>
    </build>

Changes
=======
1.0.1 (in development)

1.0.0
* Support http(s) keystore locations 
* Support proxies
