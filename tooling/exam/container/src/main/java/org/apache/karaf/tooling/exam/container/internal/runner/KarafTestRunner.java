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

/*
 * Copyright 2010 - 2011 Toni Menzel.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.karaf.tooling.exam.container.internal.runner;

import com.google.common.collect.Sets;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Constants;
import org.ops4j.pax.exam.ExamConfigurationException;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.junit.Configuration;
import org.ops4j.pax.exam.junit.ExamFactory;
import org.ops4j.pax.exam.spi.DefaultExamSystem;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.exam.spi.intern.DefaultTestAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KarafTestRunner extends BlockJUnit4ClassRunner {

    private static Logger LOG = LoggerFactory.getLogger(BlockJUnit4ClassRunner.class);

    private final Map<FrameworkMethod, List<TestContainer>> children = new HashMap<FrameworkMethod, List<TestContainer>>();

    private ExamSystem m_system;

    public KarafTestRunner(Class<?> klass) throws Exception {
        super(klass);
        prepareReactor();
    }

    @Override
    public void run(RunNotifier notifier) {
        try {
            super.run(notifier);
        } catch (Exception e) {
            throw new TestContainerException("Problem interacting with reactor.", e);
        } finally {
            m_system.clear();
        }
    }

    /**
     * We overwrite those with reactor content
     */
    @Override
    protected List<FrameworkMethod> getChildren() {
        return Arrays.asList(children.keySet().toArray(new FrameworkMethod[children.size()]));
    }

    private synchronized void prepareReactor() throws Exception {
        ConfigurationManager cm = new ConfigurationManager();
        String systemType = cm.getProperty(Constants.EXAM_SYSTEM_KEY);
        if (Constants.EXAM_SYSTEM_DEFAULT.equals(systemType)) {
            m_system = DefaultExamSystem.create(new Option[0]);
        } else {
            m_system = PaxExamRuntime.createTestSystem();
        }
        Class<?> testClass = getTestClass().getJavaClass();
        Object testClassInstance = testClass.newInstance();
        TestContainerFactory factory = getExamFactory(testClass);
        List<Set<TestContainer>> containersByConfig = new ArrayList<Set<TestContainer>>();
        for (Option[] config : getAllConfigs(testClass, testClassInstance)) {
            TestContainer[] testContainers = factory.create(m_system.fork(config));
            containersByConfig.add(Sets.newHashSet(testContainers));
        }
        // make sure to create a test-case for each combination of containers
        Set<List<TestContainer>> combos = Sets.cartesianProduct(containersByConfig);
        for (List<TestContainer> containers : combos) {
            addTestsToReactor(containers);
        }
    }

    private List<Option[]> getAllConfigs(Class<?> testClass, Object testClassInstance)
            throws IllegalAccessException, InvocationTargetException, IllegalArgumentException, IOException {
        List<Option[]> result = new ArrayList<Option[]>();
        Method[] methods = testClass.getMethods();
        for (Method m : methods) {
            Configuration conf = m.getAnnotation(Configuration.class);
            if (conf != null) {
                result.add(((Option[]) m.invoke(testClassInstance)));
            }
        }
        return result;
    }

    private String makeTestContainerListCaption(List<TestContainer> containers) {
        StringBuilder result = new StringBuilder();
        for (TestContainer tc : containers) {
            result.append(tc.toString());
            result.append("+");
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private void addTestsToReactor(List<TestContainer> containers) throws IOException, ExamConfigurationException {
        for (final FrameworkMethod frameworkMethod : super.getChildren()) {
            final DefaultTestAddress address = new DefaultTestAddress(makeTestContainerListCaption(containers), containers, frameworkMethod);
            // now, someone later may refer to that artificial FrameworkMethod. We need to be able to tell the address.
            FrameworkMethod method = new FrameworkMethod(frameworkMethod.getMethod()) {
                @Override
                public String getName() {
                    return frameworkMethod.getName() + ":" + address.caption();
                }

                @Override
                public boolean equals(Object obj) {
                    return address.equals(obj);
                }

                @Override
                public int hashCode() {
                    return address.hashCode();
                }
            };
            children.put(method, containers);
        }
    }

    private TestContainerFactory getExamFactory(Class<?> testClass)
            throws IllegalAccessException, InstantiationException {
        ExamFactory f = (ExamFactory) testClass.getAnnotation(ExamFactory.class);

        TestContainerFactory fact;
        if (f != null) {
            fact = f.value().newInstance();
        } else {
            // default:
            fact = PaxExamRuntime.getTestContainerFactory();
        }
        return fact;
    }

    protected synchronized Statement methodInvoker(final FrameworkMethod method, final Object test) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                List<TestContainer> testContainers = children.get(method);
                for (TestContainer testContainer : testContainers) {
                    // Start containers
                    testContainer.start();
                }
                method.invokeExplosively(test);
                for (TestContainer testContainer : testContainers) {
                    // Stop Containers
                    testContainer.stop();
                }
            }
        };
    }

}
