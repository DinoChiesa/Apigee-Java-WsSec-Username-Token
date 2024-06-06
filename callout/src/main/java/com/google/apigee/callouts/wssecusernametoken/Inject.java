// Copyright 2018-2023 Google LLC
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
import com.google.apigee.util.TimeResolver;
import com.google.apigee.xml.Namespaces;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
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

  java.util.concurrent.atomic.AtomicInteger idCounter =
      new java.util.concurrent.atomic.AtomicInteger(100);

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

  private String randomId() {
    return String.valueOf(idCounter.getAndIncrement());
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

    // 0. grab the Envelope and Body elements
    Element root = doc.getDocumentElement();
    if (!"Envelope".equals(root.getLocalName())) {
      throw new IllegalStateException("Not a SOAP Envelope, incorrect root element.");
    }
    String rootNs = root.getNamespaceURI();
    if (!Namespaces.soapNamespaces.contains(rootNs)) {
      throw new IllegalStateException("Not a SOAP Envelope, unsupported namespace.");
    }
    String soapns = rootNs;

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

    // 1. set up the map of namespaces
    Map<String, String> knownNamespaces = Namespaces.getExistingNamespaces(envelope);
    String wsuPrefix = declareXmlnsPrefix(envelope, knownNamespaces, Namespaces.WSU);
    String soapPrefix = declareXmlnsPrefix(envelope, knownNamespaces, soapns);
    String wssePrefix = declareXmlnsPrefix(envelope, knownNamespaces, Namespaces.WSSE);

    BiFunction<Element, String, String> wsuIdInjector =
        (elt, prefix) -> {
          String id = prefix + "-" + randomId();
          elt.setAttributeNS(Namespaces.WSU, wsuPrefix + ":Id", id);
          elt.setIdAttributeNS(Namespaces.WSU, "Id", true);
          return id;
        };

    // 2. create a nonce and createdTime, we'll need these later
    Element nonce = doc.createElementNS(Namespaces.WSSE, wssePrefix + ":Nonce");
    final byte[] nonceBytes = new byte[20];
    SecureRandom.getInstanceStrong().nextBytes(nonceBytes);
    String encodedNonce = Base64.getEncoder().encodeToString(nonceBytes);
    String createdTime = getISOTimestamp(0);

    // 3. create or get the soap:Header
    Element header = null;
    nodes = doc.getElementsByTagNameNS(soapns, "Header");
    if (nodes.getLength() == 0) {
      header = doc.createElementNS(soapns, soapPrefix + ":Header");
      envelope.insertBefore(header, body);
    } else {
      header = (Element) nodes.item(0);
    }

    // 4. create or get the WS-Security element within the header
    Element wssecHeader = null;
    nodes = header.getElementsByTagNameNS(Namespaces.WSSE, "Security");
    if (nodes.getLength() == 0) {
      wssecHeader = doc.createElementNS(Namespaces.WSSE, wssePrefix + ":Security");
      wssecHeader.setAttributeNS(soapns, soapPrefix + ":mustUnderstand", "1");

      if (header.getFirstChild() != null) {
        header.insertBefore(wssecHeader, header.getFirstChild());
      } else {
        header.appendChild(wssecHeader);
      }
    } else {
      wssecHeader = (Element) nodes.item(0);
    }

    // 5a. optionally embed a Timestamp element under the wssecHeader element
    if (policyConfiguration.expiresInSeconds > 0) {
      Element timestamp = doc.createElementNS(Namespaces.WSU, wsuPrefix + ":Timestamp");
      wsuIdInjector.apply(timestamp, "TS");
      wssecHeader.appendChild(timestamp);

      // 5b. embed a Created element into the Timestamp
      Element timestampCreated = doc.createElementNS(Namespaces.WSU, wsuPrefix + ":Created");
      timestampCreated.setTextContent(createdTime);
      timestamp.appendChild(timestampCreated);

      // 5c. optionally, embed an Expires element into the Timestamp
      Element expires = doc.createElementNS(Namespaces.WSU, wsuPrefix + ":Expires");
      expires.setTextContent(getISOTimestamp(policyConfiguration.expiresInSeconds));
      timestamp.appendChild(expires);
    }

    // 6. embed a UsernameToken element under the wssecHeader element
    Element usernameToken = doc.createElementNS(Namespaces.WSSE, wssePrefix + ":UsernameToken");
    wsuIdInjector.apply(usernameToken, "UT");
    wssecHeader.appendChild(usernameToken);

    // 7a. add the username
    Element username = doc.createElementNS(Namespaces.WSSE, wssePrefix + ":Username");
    username.setTextContent(policyConfiguration.username);
    usernameToken.appendChild(username);

    // 7b. add the password, digest or plain text
    Element password = doc.createElementNS(Namespaces.WSSE, wssePrefix + ":Password");
    if (policyConfiguration.passwordEncoding == PasswordEncoding.DIGEST) {
      password.setAttribute("Type", Namespaces.USERNAMETOKEN_PASSWORDDIGEST);
      String aggregate = encodedNonce + createdTime + policyConfiguration.password;
      String passwordDigest =
          Base64.getEncoder()
              .encodeToString(
                  MessageDigest.getInstance("SHA1")
                      .digest(aggregate.getBytes(StandardCharsets.UTF_8)));
      password.setTextContent(passwordDigest);
    } else {
      password.setAttribute("Type", Namespaces.USERNAMETOKEN_PASSWORDTEXT);
      password.setTextContent(policyConfiguration.password);
    }
    usernameToken.appendChild(password);

    // 7c. embed a Created element under UsernameToken
    if (policyConfiguration.passwordEncoding == PasswordEncoding.DIGEST
        || policyConfiguration.wantCreatedTime) {
      Element tokenCreated = doc.createElementNS(Namespaces.WSU, wsuPrefix + ":Created");
      tokenCreated.setTextContent(createdTime);
      usernameToken.appendChild(tokenCreated);
    }

    // 7d. embed the Nonce
    if (policyConfiguration.passwordEncoding == PasswordEncoding.DIGEST
        || policyConfiguration.wantNonce) {
      nonce.setTextContent(encodedNonce);
      nonce.setAttribute("EncodingType", Namespaces.BASE64BINARY);
      usernameToken.appendChild(nonce);
    }

    // 8. emit the resulting document
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    Transformer transformer = TransformerFactory.newInstance().newTransformer();
    transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    transformer.transform(new DOMSource(doc), new StreamResult(baos));
    return new String(baos.toByteArray(), StandardCharsets.UTF_8);
  }

  private int getExpiresIn(MessageContext msgCtxt) throws Exception {
    String expiryString = getSimpleOptionalProperty("expiry", msgCtxt);
    if (expiryString == null) return 0;
    expiryString = expiryString.trim();
    Long durationInMilliseconds = TimeResolver.resolveExpression(expiryString);
    if (durationInMilliseconds < 0L) return 0;
    return ((Long) (durationInMilliseconds / 1000L)).intValue();
  }

  private Optional<Boolean> getNamedOptionalBoolean(String name, MessageContext msgCtxt)
      throws Exception {
    String value = getSimpleOptionalProperty(name, msgCtxt);
    if (value == null) return Optional.empty();
    return Optional.of(value.trim().toLowerCase().equals("true"));
  }

  private Optional<Boolean> getWantNonceOptional(MessageContext msgCtxt) throws Exception {
    return getNamedOptionalBoolean("want-nonce", msgCtxt);
  }

  private Optional<Boolean> getWantCreatedTimeOptional(MessageContext msgCtxt) throws Exception {
    return getNamedOptionalBoolean("want-created-time", msgCtxt);
  }

  static class PolicyConfiguration {
    public String username; // required
    public String password; // required
    public PasswordEncoding passwordEncoding;
    public int expiresInSeconds = 0; // optional
    public boolean wantNonce; // optional
    public boolean wantCreatedTime; // optional

    public PolicyConfiguration() {
      wantNonce = false;
      wantCreatedTime = false;
    }

    public PolicyConfiguration withUsername(String username) {
      this.username = username;
      return this;
    }

    public PolicyConfiguration withPassword(String password) {
      this.password = password;
      return this;
    }

    public PolicyConfiguration withPasswordEncoding(PasswordEncoding passwordEncoding) {
      this.passwordEncoding = passwordEncoding;
      return this;
    }

    public PolicyConfiguration withExpiresIn(int expiresIn) {
      this.expiresInSeconds = expiresIn;
      return this;
    }

    public PolicyConfiguration withWantNonce(boolean wantNonce) {
      this.wantNonce = wantNonce;
      return this;
    }

    public PolicyConfiguration withWantCreatedTime(boolean wantCreatedTime) {
      this.wantCreatedTime = wantCreatedTime;
      return this;
    }
  }

  public ExecutionResult execute(final MessageContext msgCtxt, final ExecutionContext execContext) {
    try {
      Document document = getDocument(msgCtxt);

      PolicyConfiguration policyConfiguration =
          new PolicyConfiguration()
              .withUsername(getUsername(msgCtxt))
              .withPassword(getPassword(msgCtxt))
              .withPasswordEncoding(getPasswordEncoding(msgCtxt))
              .withExpiresIn(getExpiresIn(msgCtxt));

      getWantNonceOptional(msgCtxt)
          .ifPresent(wantNonce -> policyConfiguration.withWantNonce(wantNonce));
      getWantCreatedTimeOptional(msgCtxt)
          .ifPresent(wantCreatedTime -> policyConfiguration.withWantCreatedTime(wantCreatedTime));

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
