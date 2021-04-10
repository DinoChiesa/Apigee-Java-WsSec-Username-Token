package com.google.apigee.callouts.wssecusernametoken;

import com.apigee.flow.execution.ExecutionResult;
import com.google.apigee.xml.Namespaces;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class TestWssecUsernameTokenInjectCallout extends CalloutTestBase {
  private static final String simpleSoap1 =
      "<soapenv:Envelope xmlns:ns1='http://ws.example.com/' xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>"
          + "  <soapenv:Body>"
          + "    <ns1:sumResponse>"
          + "      <ns1:return>9</ns1:return>"
          + "    </ns1:sumResponse>"
          + "  </soapenv:Body>"
          + "</soapenv:Envelope>";

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
    msgCtxt.setVariable("message-content", simpleSoap1);

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

    msgCtxt.setVariable("message.content", simpleSoap1);

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
    msgCtxt.setVariable("message.content", simpleSoap1);
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
  public void validResult() throws Exception {
      final String appliedUsername = "emil@gaffanon.com";
      final String appliedPassword = "Albatross1";
    String method = "validResult() ";
    msgCtxt.setVariable("message.content", simpleSoap1);
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
    NodeList nl = doc.getElementsByTagNameNS(Namespaces.WSSEC, "UsernameToken");
    Assert.assertEquals(nl.getLength(), 1, method + "UsernameToken element");
    Element usernameToken = (Element) nl.item(0);

    // username
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSEC, "Username");
    Assert.assertEquals(nl.getLength(), 1, method + "Username element");
    Element element = (Element) nl.item(0);
    String username = element.getTextContent();
    Assert.assertEquals(username, appliedUsername);

    // password
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSEC, "Password");
    Assert.assertEquals(nl.getLength(), 1, method + "Password element");
    element = (Element) nl.item(0);
    String password = element.getTextContent();
    Assert.assertEquals(password, appliedPassword);

    // Nonce
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSSEC, "Nonce");
    Assert.assertEquals(nl.getLength(), 1, method + "Nonce element");

    // Created
    nl = usernameToken.getElementsByTagNameNS(Namespaces.WSU, "Created");
    Assert.assertEquals(nl.getLength(), 1, method + "Created element");

  }
}
