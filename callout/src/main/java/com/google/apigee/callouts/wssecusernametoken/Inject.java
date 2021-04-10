// Copyright 2018-2021 Google LLC
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

package com.google.apigee.callouts.wssecusernametoken;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.execution.ExecutionResult;
import com.apigee.flow.execution.spi.Execution;
import com.apigee.flow.message.MessageContext;
import com.google.apigee.xml.Namespaces;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Inject extends WssecUsernameTokenCalloutBase implements Execution {

  public Inject(Map properties) {
    super(properties);
  }

  // public static String toPrettyString(Document document, int indent) {
  //   try {
  //     // Remove whitespaces outside tags
  //     document.normalize();
  //     XPath xPath = XPathFactory.newInstance().newXPath();
  //     NodeList nodeList =
  //         (NodeList)
  //             xPath.evaluate("//text()[normalize-space()='']", document, XPathConstants.NODESET);
  //
  //     for (int i = 0; i < nodeList.getLength(); ++i) {
  //       Node node = nodeList.item(i);
  //       node.getParentNode().removeChild(node);
  //     }
  //
  //     // Setup pretty print options
  //     TransformerFactory transformerFactory = TransformerFactory.newInstance();
  //     transformerFactory.setAttribute("indent-number", indent);
  //     Transformer transformer = transformerFactory.newTransformer();
  //     transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
  //     transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
  //     transformer.setOutputProperty(OutputKeys.INDENT, "yes");
  //
  //     // Return pretty print xml string
  //     StringWriter stringWriter = new StringWriter();
  //     transformer.transform(new DOMSource(document), new StreamResult(stringWriter));
  //     return stringWriter.toString();
  //   } catch (Exception e) {
  //     throw new RuntimeException(e);
  //   }
  // }

  // public static Element getFirstChildElement(Element element) {
  //   for (Node currentChild = element.getFirstChild();
  //        currentChild != null;
  //        currentChild = currentChild.getNextSibling()) {
  //     if (currentChild instanceof Element) {
  //       return (Element) currentChild;
  //     }
  //   }
  //   return null;
  // }

  private int nsCounter = 1;

  private String declareXmlnsPrefix(
      Element elt, Map<String, String> knownNamespaces, String namespaceURIToAdd) {
    // search here for an existing prefix with the specified URI.
    String prefix = knownNamespaces.get(namespaceURIToAdd);
    if (prefix != null) {
      return prefix;
    }

    // find the default prefix for the specified URI.
    prefix = Namespaces.defaultPrefixes.get(namespaceURIToAdd);
    if (prefix == null) {
      prefix = "ns" + nsCounter++;
    }

    if (elt != null) {
      elt.setAttributeNS(Namespaces.XMLNS, "xmlns:" + prefix, namespaceURIToAdd);
    }
    return prefix;
  }

  private static String getISOTimestamp(int offsetFromNow) {
    ZonedDateTime zdt = ZonedDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.SECONDS);
    if (offsetFromNow != 0) zdt = zdt.plusSeconds(offsetFromNow);
    return zdt.format(DateTimeFormatter.ISO_INSTANT);
    // return ZonedDateTime.ofInstant(Instant.ofEpochSecond(secondsSinceEpoch), ZoneOffset.UTC)
    //     .format(DateTimeFormatter.ISO_INSTANT);
  }

  private String injectToken(Document doc, PolicyConfiguration policyConfiguration)
      throws NoSuchAlgorithmException, TransformerConfigurationException, TransformerException {
    String soapns = Namespaces.SOAP10;

    NodeList nodes = doc.getElementsByTagNameNS(soapns, "Envelope");
    if (nodes.getLength() != 1) {
      return null;
    }
    Element envelope = (Element) nodes.item(0);

    nodes = envelope.getElementsByTagNameNS(soapns, "Body");
    if (nodes.getLength() != 1) {
      return null;
    }

    Element body = (Element) nodes.item(0);

    Map<String, String> knownNamespaces = Namespaces.getExistingNamespaces(envelope);
    String wsuPrefix = declareXmlnsPrefix(envelope, knownNamespaces, Namespaces.WSU);
    String soapPrefix = declareXmlnsPrefix(envelope, knownNamespaces, Namespaces.SOAP10);
    String wssePrefix = declareXmlnsPrefix(envelope, knownNamespaces, Namespaces.WSSEC);

    // 1. create or get the soap:Header
    Element header = null;
    nodes = doc.getElementsByTagNameNS(soapns, "Header");
    if (nodes.getLength() == 0) {
      header = doc.createElementNS(soapns, soapPrefix + ":Header");
      envelope.insertBefore(header, body);
    } else {
      header = (Element) nodes.item(0);
    }

    // 2. create or get the WS-Security element within the header
    Element wssecHeader = null;
    nodes = header.getElementsByTagNameNS(Namespaces.WSSEC, "Security");
    if (nodes.getLength() == 0) {
      wssecHeader = doc.createElementNS(Namespaces.WSSEC, wssePrefix + ":Security");
      wssecHeader.setAttributeNS(soapns, soapPrefix + ":mustUnderstand", "1");
      header.appendChild(wssecHeader);
    } else {
      wssecHeader = (Element) nodes.item(0);
    }

    // 3. embed a UsernameToken element under the wssecHeader element
    Element usernameToken = doc.createElementNS(Namespaces.WSSEC, wssePrefix + ":UsernameToken");
    String tokenId = "UsernameToken-" + java.util.UUID.randomUUID().toString();
    usernameToken.setAttributeNS(Namespaces.WSU, wsuPrefix + ":Id", tokenId);
    usernameToken.setIdAttributeNS(Namespaces.WSU, "Id", true);
    wssecHeader.appendChild(usernameToken);

    // 4. add the username and password
    Element username = doc.createElementNS(Namespaces.WSSEC, wssePrefix + ":Username");
    username.setTextContent(policyConfiguration.username);
    usernameToken.appendChild(username);

    Element password = doc.createElementNS(Namespaces.WSSEC, wssePrefix + ":Password");
    password.setAttribute(
        "Type",
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText");
    password.setTextContent(policyConfiguration.password);
    usernameToken.appendChild(password);

    // 5. embed a Created element under UsernameToken
    Element created = doc.createElementNS(Namespaces.WSU, wsuPrefix + ":Created");
    created.setTextContent(getISOTimestamp(0));
    usernameToken.appendChild(created);

    // 6. embed a Nonce
    Element nonce = doc.createElementNS(Namespaces.WSSEC, wssePrefix + ":Nonce");
    final byte[] nonceBytes = new byte[20];
    SecureRandom.getInstanceStrong().nextBytes(nonceBytes);
    String encodedNonce = Base64.getEncoder().encodeToString(nonceBytes);
    nonce.setTextContent(encodedNonce);
    nonce.setAttribute(
        "EncodingType",
        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary");
    usernameToken.appendChild(nonce);

    // emit the resulting document
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.transform(new DOMSource(doc), new StreamResult(baos));
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  static class PolicyConfiguration {
    public String username; // required
    public String password; // required

    public PolicyConfiguration withUsername(String username) {
      this.username = username;
      return this;
    }

    public PolicyConfiguration withPassword(String password) {
      this.password = password;
      return this;
    }
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      Document document = getDocument(msgCtxt);

      PolicyConfiguration policyConfiguration =
          new PolicyConfiguration()
              .withUsername(getUsername(msgCtxt))
              .withPassword(getPassword(msgCtxt));

      String resultingXmlString = injectToken(document, policyConfiguration);
      String outputVar = getOutputVar(msgCtxt);
      msgCtxt.setVariable(outputVar, resultingXmlString);
      return ExecutionResult.SUCCESS;
    } catch (IllegalStateException exc1) {
      setExceptionVariables(exc1, msgCtxt);
      return ExecutionResult.ABORT;
    } catch (Exception e) {
      if (getDebug()) {
        String stacktrace = getStackTraceAsString(e);
        msgCtxt.setVariable(varName("stacktrace"), stacktrace);
      }
      setExceptionVariables(e, msgCtxt);
      return ExecutionResult.ABORT;
    }
  }
}
