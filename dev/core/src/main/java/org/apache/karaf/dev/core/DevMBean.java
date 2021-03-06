/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.dev.core;


/**
 * MBean providing dev actions.
 */
public interface DevMBean {

    /**
     * Get the current OSGi framework in use.
     *
     * @return the name of the OSGi framework in use.
     * @throws Exception
     */
    String getFramework();

    /**
     * change OSGi framework
     *
     * @param framework to use.
     */
    void setFramework(String framework);
    
    /**
     * Enable or diable debgging
     * @param debug enable if true
     */
    void setFrameworkDebug(boolean debug);

    /**
     * Restart Karaf and optionally clean the bundles
     * 
     * @param clean all bundle states if true
     */
    void restart(boolean clean);

}
