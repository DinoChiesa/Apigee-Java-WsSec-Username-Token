# Apigee Java Callout for WS-Security Username Token

This directory contains the Java source code and pom.xml file required
to compile a simple Java callout for Apigee, that inserts a
username token that complies with WS-Security standard into a SOAP message.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is Copyright 2018-2022, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source but you don't need to compile it in order to use it.

## Building

You do not need to build the callout in order to use it.

If you do wish to build it, you must use [maven](https://maven.apache.org/) to
build and package the jar. You need maven v3.5 at a minimum. The callout builds
with java8. It will not build with a later JDK, because of a depedency on
JMockit which requires Java8.

```
mvn clean package
```

The 'package' goal will copy the jar to the resources/java directory for the
example proxy bundle. If you want to use this in your own API Proxy, you need
to drop this JAR into the appropriate API Proxy bundle. Or include the jar as an
environment-wide or organization-wide jar via the Apigee administrative API.


## Details

There is a single jar, apigee-wssecusernametoken-20210409.jar . Within that jar,
there is a single callout classes:

* com.google.apigee.callouts.wssecusernametoken.Inject

Use this class to inject the username token into the input SOAP document.
It also will insert a nonce and a timestamp.  The resulting header is like this:

```xml
 <wsse:UsernameToken wsu:Id="UsernameToken-33966159F436ED774C158171838890544">
    <wsse:Username>MyUser</wsse:Username>
    <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">MyPassword</wsse:Password>
    <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">ID79BmTDQ5z2hLt4MQQ8RQ==</wsse:Nonce>
    <wsu:Created>2020-02-14T22:13:08.905Z</wsu:Created>
 </wsse:UsernameToken>
```

## Dependencies

None.

This Callout does not depend on WSS4J.  This callout is intended to be
usable in Apigee Cloud.

## Usage

Configure the policy this way:

```xml
<JavaCallout name='Java-WSSEC-Username-Token>
  <Properties>
    <Property name='source'>message.content</Property>
    <Property name='output-variable'>output</Property>
    <Property name='username'>{my_username}</Property>
    <Property name='password'>{my_password}</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.wssecusernametoken.Inject</ClassName>
  <ResourceURL>java://apigee-wssecusernametoken-20200409.jar</ResourceURL>
</JavaCallout>
```

The properties are:

| name                 | description |
| -------------------- | ------------ |
| source               | optional. the variable name in which to obtain the source document to sign. Defaults to message.content |
| output-variable      | optional. the variable name in which to write the signed XML. Defaults to message.content |
| username             | required. the username to inject |
| password             | required. the password to inject |


See [the example API proxy included here](./bundle) for a working example of this policy configurations.


## Example API Proxy Bundle

Deploy the API Proxy to an organization and environment using a tool like [importAndDeploy.js](https://github.com/DinoChiesa/apigee-edge-js/blob/master/examples/importAndDeploy.js)

There are some sample SOAP request documents included in this repo that you can use for demonstrations.

### Invoking the Example proxy:

* Injecting a username token

   ```
   ORG=myorgname
   ENV=myenv
   curl -i https://${ORG}-${ENV}.apigee.net/wssec-username/inject  -H content-type:application/xml \
       --data-binary @./sample-data/request1.xml
   ```

### Before and After Example

Supposing the input XML looks like this:

```xml
<soapenv:Envelope
    xmlns:ns1='http://ws.example.com/'
    xmlns:soapenv='http://schemas.xmlsoap.org/soap/envelope/'>
  <soapenv:Body>
    <ns1:sumResponse>
      <ns1:return>9</ns1:return>
    </ns1:sumResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

Then,
the modified payload looks like this:

```xml
<soapenv:Envelope
    xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
    xmlns:wssec="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd"
    xmlns:wsu="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd"
    xmlns:ns1="http://ws.example.com/">
  <soapenv:Header>
    <wssec:Security soapenv:mustUnderstand="1">
      <wssec:UsernameToken wsu:Id="UsernameToken-17236a97-4ece-4c24-868f-5e23a3866444">
        <wssec:Username>emil@gaffanon.com</wssec:Username>
        <wssec:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">my_secret_password!</wssec:Password>
        <wsu:Created>2020-02-22T01:48:43Z</wsu:Created>
        <wssec:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">+GYBJvPvSFHxZRdH3G8Rxe+LKWs=</wssec:Nonce>
      </wssec:UsernameToken>
    </wssec:Security>
  </soapenv:Header>
  <soapenv:Body>
    <ns1:sumResponse>
      <ns1:return>9</ns1:return>
    </ns1:sumResponse>
  </soapenv:Body>
</soapenv:Envelope>
```

(This example has been prettified.)


## Bugs

none?
