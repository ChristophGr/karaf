/**
 * Licensed to the Austrian Association for Software Tool Integration (AASTI)
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. The AASTI licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.exam.regression;

import org.apache.karaf.tooling.exam.container.internal.runner.KarafTestRunner;
import org.apache.sshd.ClientSession;
import org.apache.sshd.SshClient;
import org.apache.sshd.client.future.ConnectFuture;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.Configuration;

import java.util.concurrent.TimeUnit;

import static org.apache.karaf.tooling.exam.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.CoreOptions.maven;

@RunWith(KarafTestRunner.class)
public class SSHTest {

    private String version = "2.2.4";

    @Configuration
    public Option[] config() {
        return new Option[]{
                karafDistributionConfiguration().frameworkUrl(
                        maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("zip").versionAsInProject()),
                karafDistributionConfiguration().frameworkUrl(
                        maven().groupId("org.apache.karaf").artifactId("apache-karaf").type("zip").version(version))
                        .karafVersion(version).name("Apache Karaf")
        };
    }

    @Test
    public void connectViaSSH_shouldWork() throws Exception {
        String host = "localhost";
        int port = 8101;
        SshClient client = SshClient.setUpDefaultClient();
        client.start();
        ConnectFuture connectFuture = client.connect(host, port);
        if (!connectFuture.await(30, TimeUnit.SECONDS)) {
            throw (Exception) connectFuture.getException();
        }
        ClientSession session = connectFuture.getSession();
        session.close(false);
        client.stop();
    }
}
