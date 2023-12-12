// Copyright 2019-2023 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.xml;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Namespaces {
  public static final String WSU =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd";
  public static final String SOAP11 = "http://schemas.xmlsoap.org/soap/envelope/";
  public static final String SOAP12 = "http://www.w3.org/2003/05/soap-envelope";
  public static final String WSSE =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd";
  public static final String WSSE11 =
      "http://docs.oasis-open.org/wss/2005/xx/oasis-2005xx-wss-wssecurity-secext-1.1.xsd";
  public static final String XMLNS = "http://www.w3.org/2000/xmlns/";
  public static final String XMLDSIG = "http://www.w3.org/2000/09/xmldsig#";

  public static final Map<String, String> defaultPrefixes;
  public static final Set<String> soapNamespaces;

  public static final String USERNAMETOKEN_PASSWORDTEXT =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText";
  public static final String USERNAMETOKEN_PASSWORDDIGEST =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordDigest";
  public static final String BASE64BINARY =
      "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary";

  static {
    Map<String, String> map1 = new HashMap<String, String>();
    map1.put(WSU, "wsu");
    map1.put(SOAP11, "soap");
    map1.put(SOAP12, "soap");
    map1.put(WSSE, "wsse");
    map1.put(WSSE11, "wsse11");
    map1.put(XMLDSIG, "ds");
    defaultPrefixes = Collections.synchronizedMap(map1);
  }

  static {
    Set<String> set1 = new HashSet<String>();
    set1.add(SOAP11);
    set1.add(SOAP12);
    soapNamespaces = Collections.unmodifiableSet(set1);
  }

  public static Map<String, String> getExistingNamespaces(Element element) {
    Map<String, String> knownNamespaces = new HashMap<String, String>();
    NamedNodeMap attributes = element.getAttributes();
    if (attributes != null) {
      for (int i = 0; i < attributes.getLength(); i++) {
        Node node = attributes.item(i);
        if (node.getNodeType() == Node.ATTRIBUTE_NODE) {
          String name = node.getNodeName();
          if (name.startsWith("xmlns:")) {
            String value = node.getNodeValue();
            knownNamespaces.put(value, name.substring(6));
          }
        }
      }
    }
    return Collections.unmodifiableMap(knownNamespaces); // key:namespace, value:prefix
  }
}
