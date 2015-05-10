/*
 * Copyright 2014 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.modifier.connector.jdkhttpconnector;

import java.security.ProtectionDomain;

import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matcher;
import com.navercorp.pinpoint.bootstrap.instrument.matcher.Matchers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.navercorp.pinpoint.bootstrap.Agent;
import com.navercorp.pinpoint.bootstrap.instrument.ByteCodeInstrumentor;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentClass;
import com.navercorp.pinpoint.bootstrap.instrument.InstrumentException;
import com.navercorp.pinpoint.profiler.modifier.AbstractModifier;
import com.navercorp.pinpoint.profiler.modifier.connector.jdkhttpconnector.interceptor.ConnectMethodInterceptor;

/**
 * TODO Fix class loader issue.
 * @author netspider
 * 
 */
public class HttpURLConnectionModifier extends AbstractModifier {
    private final static String SCOPE = "HttpURLConnectoin";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpURLConnectionModifier(ByteCodeInstrumentor byteCodeInstrumentor, Agent agent) {
        super(byteCodeInstrumentor, agent);
    }

    public Matcher getMatcher() {
        return Matchers.newClassNameMatcher("sun/net/www/protocol/http/HttpURLConnection");
    }

    public byte[] modify(ClassLoader classLoader, String javassistClassName, ProtectionDomain protectedDomain, byte[] classFileBuffer) {
        if (logger.isInfoEnabled()) {
            logger.info("Modifying. {}", javassistClassName);
        }

        try {
            InstrumentClass aClass = byteCodeInstrumentor.getClass(classLoader, javassistClassName, classFileBuffer);
            
            ConnectMethodInterceptor connectMethodInterceptor = new ConnectMethodInterceptor();
            aClass.addGroupInterceptor("connect", null, connectMethodInterceptor, SCOPE);
            
            ConnectMethodInterceptor getInputStreamInterceptor = new ConnectMethodInterceptor();
            aClass.addGroupInterceptor("getInputStream", null, getInputStreamInterceptor, SCOPE);
            
            ConnectMethodInterceptor getOutputStreamInterceptor = new ConnectMethodInterceptor();
            aClass.addGroupInterceptor("getOutputStream", null, getOutputStreamInterceptor, SCOPE);
            
            aClass.addGetter("__isConnected", "connected", "boolean");
            
            if (aClass.hasField("connecting", "boolean")) {
                aClass.addGetter("__isConnecting", "connecting", "boolean");
            }
            
            return aClass.toBytecode();
        } catch (InstrumentException e) {
            logger.warn("HttpURLConnectionModifier fail. Caused:", e.getMessage(), e);
            return null;
        }
    }
}