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

package com.google.apigee.callouts.wssecusernametoken;

import com.apigee.flow.execution.ExecutionResult;
import com.google.apigee.xml.Namespaces;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestWssecUsernameTokenInjectCallout extends CalloutTestBase {
  private static final String simpleSoap11 =
      "<soapenv:Envelope xmlns:ns1='http://ws.example.com/'\n"
          + "  xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>\n"
          + "  <soapenv:Body>\n"
          + "    <ns1:sumResponse>\n"
          + "      <ns1:return>9</ns1:return>\n"
          + "    </ns1:sumResponse>\n"
          + "  </soapenv:Body>\n"
          + "</soapenv:Envelope>";

  private static final String simpleSoap12 =
      "<soap:Envelope xmlns:soap='http://www.w3.org/2003/05/soap-envelope'\n"
          + "               xmlns:ns='http://xmlns.example.com/Schemas/Common/header/1.0.0'\n"
          + "              "
          + " xmlns:v1='http://www.example.com/schemas/storeops/tux/reservation/v1'>\n"
          + "  <soap:Header>\n"
          + "    <ns:Header>\n"
          + "      <ns:Source>Web</ns:Source>\n"
          + "      <ns:Domain>STOREOPS</ns:Domain>\n"
          + "      <ns:UserID>DFGH18</ns:UserID>\n"
          + "      <ns:Password>TEST123</ns:Password>\n"
          + "      <ns:Version>1.0</ns:Version>\n"
          + "      <ns:IPAddress>123.45.67.89</ns:IPAddress>\n"
          + "    </ns:Header>\n"
          + "  </soap:Header>\n"
          + "  <soap:Body>\n"
          + "    <v1:GetReservationRequest>\n"
          + "      <v1:ID>115097597</v1:ID>\n"
          + "    </v1:GetReservationRequest>\n"
          + "  </soap:Body>\n"
          + "</soap:Envelope>\n";

  private static Document docFromStream(InputStream inputStream)
      throws IOException, ParserConfigurationException, SAXException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    Document doc = dbf.newDocumentBuilder().parse(inputStream);
    return doc;
  }

  @Test
  public void emptySource() throws Exception {
    String method = "emptySource() ";
    String expectedError = "source variable resolves to null";
    msgCtxt.setVariable("message-content", simpleSoap11);

    Map<String, String> props = new HashMap<String, String>();
    props.put("source", "not-message.content");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");
  }

  @Test
  public void missingUsername() throws Exception {
    String method = "missingUsername() ";
    String expectedError = "username resolves to an empty string";

    msgCtxt.setVariable("message.content", simpleSoap11);

    Map<String, String> props = new HashMap<String, String>();
    props.put("source", "message.content");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNotNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    // System.out.printf("expected error: %s\n", errorOutput);
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");
  }

  @Test
  public void missingPassword() throws Exception {
    String method = "missingPassword() ";
    String expectedError = "password resolves to an empty string";
    msgCtxt.setVariable("message.content", simpleSoap11);
    msgCtxt.setVariable("my-username", "emil@gaffanon.com");

    Map<String, String> props = new HashMap<String, String>();
    // props.put("debug", "true");
    props.put("source", "message.content");
    props.put("username", "{my-username}");
    props.put("output-variable", "output");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.ABORT, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNotNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNotNull(errorOutput, "errorOutput");
    Assert.assertEquals(errorOutput, expectedError, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");
  }

  @Test
  public void validResult_noNonce_noCreated() throws Exception {
    validResult(false, false);
  }

  @Test
  public void validResult_noNonce_withCreated() throws Exception {
    validResult(false, true);
  }

  @Test
  public void validResult_withNonce_noCreated() throws Exception {
    validResult(false, true);
  }

  @Test
  public void validResult_withNonce_withCreated() throws Exception {
    validResult(true, true);
  }

  private void validResult(boolean wantNonce, boolean wantCreatedTime) throws Exception {

    final String appliedUsername = "emil@gaffanon.com";
    final String appliedPassword = "Albatross1";
    String method = "validResult() ";
    msgCtxt.setVariable("message.content", simpleSoap11);
    msgCtxt.setVariable("my-username", appliedUsername);
    msgCtxt.setVariable("my-password", appliedPassword);

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");
    props.put("username", "{my-username}");
    props.put("password", "{my-password}");
    props.put("want-nonce", String.valueOf(wantNonce));
    props.put("want-created-time", String.valueOf(wantCreatedTime));
    props.put("output-variable", "output");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNull(errorOutput, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");

    String output = (String) msgCtxt.getVariable("output");
    System.out.printf("** Output:\n" + output + "\n");

    Document doc = docFromStream(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));

    // token
    NodeList nl = doc.getElementsByTagNameNS(Namespaces.WSSE, "UsernameToken");
    Assert.assertEquals(nl.getLength(), 1, method + "UsernameToken element");
    Element usernameToken = (Element) nl.item(0);

    // username
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Username");
    Assert.assertEquals(nl.getLength(), 1, method + "Username element");
    Element element = (Element) nl.item(0);
    String username = element.getTextContent();
    Assert.assertEquals(username, appliedUsername);

    // password
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Password");
    Assert.assertEquals(nl.getLength(), 1, method + "Password element");
    element = (Element) nl.item(0);
    String password = element.getTextContent();
    Assert.assertEquals(password, appliedPassword);

    // Nonce
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Nonce");
    Assert.assertEquals(nl.getLength(), wantNonce ? 1 : 0, method + "Nonce element");

    // Created
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSU, "Created");
    Assert.assertEquals(nl.getLength(), wantCreatedTime ? 1 : 0, method + "Created element");
  }

  @Test
  public void withDigestPassword() throws Exception {
    final String appliedUsername = "emil@gaffanon.com";
    final String appliedPassword = "Albatross1";
    String method = "validResult() ";
    msgCtxt.setVariable("message.content", simpleSoap11);
    msgCtxt.setVariable("my-username", appliedUsername);
    msgCtxt.setVariable("my-password", appliedPassword);
    msgCtxt.setVariable("my-password-encoding", "DIGEST");

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");
    props.put("username", "{my-username}");
    props.put("password", "{my-password}");
    props.put("password-encoding", "{my-password-encoding}");
    props.put("output-variable", "output");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNull(errorOutput, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");

    String output = (String) msgCtxt.getVariable("output");
    System.out.printf("** Output:\n" + output + "\n");

    Document doc = docFromStream(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));

    // token
    NodeList nl = doc.getElementsByTagNameNS(Namespaces.WSSE, "UsernameToken");
    Assert.assertEquals(nl.getLength(), 1, method + "UsernameToken element");
    Element usernameToken = (Element) nl.item(0);

    // username
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Username");
    Assert.assertEquals(nl.getLength(), 1, method + "Username element");
    Element element = (Element) nl.item(0);
    String username = element.getTextContent();
    Assert.assertEquals(username, appliedUsername);

    // password
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Password");
    Assert.assertEquals(nl.getLength(), 1, method + "Password element");
    element = (Element) nl.item(0);
    String resultingPasswordText = element.getTextContent();
    Assert.assertNotEquals(resultingPasswordText, appliedPassword);
    String passwordType = element.getAttribute("Type");
    Assert.assertTrue(passwordType.endsWith("Digest"));

    // Nonce
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Nonce");
    Assert.assertEquals(nl.getLength(), 1, method + "Nonce element");
    String nonce = ((Element) (nl.item(0))).getTextContent();

    // Created
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSU, "Created");
    Assert.assertEquals(nl.getLength(), 1, method + "Created element");
    String created = ((Element) (nl.item(0))).getTextContent();

    String s = nonce + created + appliedPassword;
    String computedPasswordDigest =
        Base64.getEncoder()
            .encodeToString(
                MessageDigest.getInstance("SHA1").digest(s.getBytes(StandardCharsets.UTF_8)));
    Assert.assertEquals(resultingPasswordText, computedPasswordDigest);
  }

  @Test
  public void validResult_soap12() throws Exception {
    final String appliedUsername = "emil@gaffanon.com";
    final String appliedPassword = "Albatross1";
    String method = "validResult_soap12() ";
    msgCtxt.setVariable("message.content", simpleSoap12);
    msgCtxt.setVariable("my-username", appliedUsername);
    msgCtxt.setVariable("my-password", appliedPassword);

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");
    props.put("username", "{my-username}");
    props.put("password", "{my-password}");
    props.put("output-variable", "output");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNull(errorOutput, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");

    String output = (String) msgCtxt.getVariable("output");
    System.out.printf("** Output:\n" + output + "\n");

    Document doc = docFromStream(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));

    // token
    NodeList nl = doc.getElementsByTagNameNS(Namespaces.WSSE, "UsernameToken");
    Assert.assertEquals(nl.getLength(), 1, method + "UsernameToken element");
    Element usernameToken = (Element) nl.item(0);

    // username
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Username");
    Assert.assertEquals(nl.getLength(), 1, method + "Username element");
    Element element = (Element) nl.item(0);
    String username = element.getTextContent();
    Assert.assertEquals(username, appliedUsername);

    // password
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Password");
    Assert.assertEquals(nl.getLength(), 1, method + "Password element");
    element = (Element) nl.item(0);
    String password = element.getTextContent();
    Assert.assertEquals(password, appliedPassword);

    // Nonce
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSE, "Nonce");
    Assert.assertEquals(nl.getLength(), 0, method + "Nonce element");

    // Created
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSU, "Created");
    Assert.assertEquals(nl.getLength(), 0, method + "Created element");
  }

  @Test
  public void withTimestamp_soap12_withCreated() throws Exception {
    withTimestamp_soap12(true);
  }

  @Test
  public void withTimestamp_soap12_noCreated() throws Exception {
    withTimestamp_soap12(false);
  }

  private void withTimestamp_soap12(boolean wantUsernameTokenCreatedTime) throws Exception {
    final String appliedUsername = "emil@gaffanon.com";
    final String appliedPassword = "Albatross1";
    String method = "withTimestamp_soap12() ";
    msgCtxt.setVariable("message.content", simpleSoap12);
    msgCtxt.setVariable("my-username", appliedUsername);
    msgCtxt.setVariable("my-password", appliedPassword);

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");
    props.put("username", "{my-username}");
    props.put("password", "{my-password}");
    props.put("want-created-time", String.valueOf(wantUsernameTokenCreatedTime));
    props.put("expiry", "300s");
    props.put("output-variable", "output");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNull(errorOutput, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");

    String output = (String) msgCtxt.getVariable("output");
    System.out.printf("** Output:\n" + output + "\n");

    Document doc = docFromStream(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));

    // timestamp
    NodeList nl = doc.getElementsByTagNameNS(Namespaces.WSU, "Timestamp");
    Assert.assertEquals(nl.getLength(), 1, method + "Timestamp element");
    Element timestamp = (Element) nl.item(0);

    // token
    nl = doc.getElementsByTagNameNS(Namespaces.WSSE, "UsernameToken");
    Assert.assertEquals(nl.getLength(), 1, method + "UsernameToken element");
    Element usernameToken = (Element) nl.item(0);

    // timestamp-created
    nl = timestamp.getElementsByTagNameNS(Namespaces.WSU, "Created");
    Assert.assertEquals(nl.getLength(), 1, method + "Timestamp/Created element");
    Element timestampCreatedElement = (Element) nl.item(0);
    String timestampCreated = timestampCreatedElement.getTextContent();
    Assert.assertNotNull(timestampCreated, "timestampCreated");

    // token-created
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSU, "Created");
    Assert.assertEquals(
        nl.getLength(),
        wantUsernameTokenCreatedTime ? 1 : 0,
        method + "UsernameToken/Created element");
    if (wantUsernameTokenCreatedTime) {
      Element tokenCreatedElement = (Element) nl.item(0);
      String tokenCreated = tokenCreatedElement.getTextContent();
      Assert.assertNotNull(tokenCreated, "tokenCreated");

      Assert.assertEquals(tokenCreated, timestampCreated);
    }
  }

  @Test
  public void withNoTimestamp_soap12() throws Exception {
    final String appliedUsername = "emil@gaffanon.com";
    final String appliedPassword = "Albatross1";
    String method = "withTimestamp_soap12() ";
    msgCtxt.setVariable("message.content", simpleSoap12);
    msgCtxt.setVariable("my-username", appliedUsername);
    msgCtxt.setVariable("my-password", appliedPassword);

    Map<String, String> props = new HashMap<String, String>();
    props.put("debug", "true");
    props.put("source", "message.content");
    props.put("username", "{my-username}");
    props.put("password", "{my-password}");
    // props.put("expiry", "300s");
    props.put("output-variable", "output");

    Inject callout = new Inject(props);

    // execute and retrieve output
    ExecutionResult actualResult = callout.execute(msgCtxt, exeCtxt);
    Assert.assertEquals(actualResult, ExecutionResult.SUCCESS, "result not as expected");
    Object exception = msgCtxt.getVariable("wssec_exception");
    Assert.assertNull(exception, method + "exception");
    Object errorOutput = msgCtxt.getVariable("wssec_error");
    Assert.assertNull(errorOutput, "error not as expected");
    Object stacktrace = msgCtxt.getVariable("wssec_stacktrace");
    Assert.assertNull(stacktrace, method + "stacktrace");

    String output = (String) msgCtxt.getVariable("output");
    System.out.printf("** Output:\n" + output + "\n");

    Document doc = docFromStream(new ByteArrayInputStream(output.getBytes(StandardCharsets.UTF_8)));

    // timestamp
    NodeList nl = doc.getElementsByTagNameNS(Namespaces.WSU, "Timestamp");
    Assert.assertEquals(nl.getLength(), 0, method + "Timestamp element");
  }
}
