/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.coheigea.cxf.ocsp.wssec;

import java.net.URL;
import java.security.Security;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;

import org.apache.cxf.Bus;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.example.contract.doubleit.DoubleItPortType;
import org.junit.BeforeClass;

/**
 * A test-case an asymmetric WS-Security client request, where the service uses OCSP to validate
 * that the client's certificate is valid.
 * 
 * Prerequisite: Launch OpenSSL via (pass phrase: security):
 * 
 * openssl ocsp -index ca.db.index -port 12345 -text -rkey wss40CAKey.pem -CA wss40CA.pem -rsigner wss40CA.pem
 */
public class WSSecurityOCSPTest extends AbstractBusClientServerTestBase {
    
    private static final String NAMESPACE = "http://www.example.org/contract/DoubleIt";
    private static final QName SERVICE_QNAME = new QName(NAMESPACE, "DoubleItService");
    
    private static final String PORT = allocatePort(Server.class);
    
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue(
                   "Server failed to launch",
                   // run the server in the same process
                   // set this to false to fork
                   launchServer(Server.class, true)
        );
    }
   
    @org.junit.Test
    public void testAsymmetricOCSP() throws Exception {
        try {
            Security.setProperty("ocsp.responderURL", "http://localhost:12345");
            Security.setProperty("ocsp.enable", "true");
            
            SpringBusFactory bf = new SpringBusFactory();
            URL busFile = WSSecurityOCSPTest.class.getResource("cxf-client.xml");
    
            Bus bus = bf.createBus(busFile.toString());
            SpringBusFactory.setDefaultBus(bus);
            SpringBusFactory.setThreadDefaultBus(bus);
            
            URL wsdl = WSSecurityOCSPTest.class.getResource("DoubleIt.wsdl");
            Service service = Service.create(wsdl, SERVICE_QNAME);
            QName portQName = new QName(NAMESPACE, "DoubleItAsymmetricOCSPPort");
            DoubleItPortType transportPort =
                service.getPort(portQName, DoubleItPortType.class);
            updateAddressPort(transportPort, PORT);
            
            doubleIt(transportPort, 25);
        } finally {
            Security.setProperty("ocsp.responderURL", "");
            Security.setProperty("ocsp.enable", "false");
        }
    }
    
    private static void doubleIt(DoubleItPortType port, int numToDouble) {
        int resp = port.doubleIt(numToDouble);
        assertEquals(numToDouble * 2 , resp);
    }
    
}
