# Apigee Java Callout for WS-Security Username Token

This directory contains the Java source code and pom.xml file required
to compile a simple Java callout for Apigee, that inserts a
UsernameToken that complies with WS-Security standard into a SOAP message.

## Disclaimer

This example is not an official Google product, nor is it part of an official Google product.

## License

This material is Copyright 2018-2023, Google LLC.
and is licensed under the Apache 2.0 license. See the [LICENSE](LICENSE) file.

This code is open source. You don't need to compile it in order to use it.

## Building

You do not need to build the callout in order to use it.

If you do wish to build it, you must use [maven](https://maven.apache.org/) to
build and package the jar. You need maven v3.5 at a minimum. The callout builds
with Java 8. It will not build with a later JDK, because of a depedency on
JMockit which requires Java 8.

```
mvn clean package
```

The 'package' goal will copy the jar to the resources/java directory for the
example proxy bundle. If you want to use this in your own API Proxy, you need
to drop this JAR into the appropriate API Proxy bundle. Or include the jar as an
environment-wide or organization-wide jar via the Apigee administrative API.


## Details

There is a single jar, apigee-wssecusernametoken-20231212.jar . Within that jar,
there is a single callout class:

* com.google.apigee.callouts.wssecusernametoken.Inject

Use this class to inject the UsernameToken into the input SOAP document.  The
UsernameToken will contain an automatically-generated Nonce and a Timestamp.
The resulting header is like this:

```xml
 <wsse:UsernameToken wsu:Id="UsernameToken-100">
    <wsse:Username>MyUser</wsse:Username>
    <wsse:Password Type="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText">MyPassword</wsse:Password>
    <wsse:Nonce EncodingType="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary">ID79BmTDQ5z2hLt4MQQ8RQ==</wsse:Nonce>
    <wsu:Created>2020-02-14T22:13:08.905Z</wsu:Created>
 </wsse:UsernameToken>
```

## Dependencies

None.

This callout is intended to be usable in Apigee Cloud (X or hybrid).

## Usage

Configure the policy this way:

```xml
<JavaCallout name='Java-WSSEC-Username-Token>
  <Properties>
    <Property name='source'>message.content</Property>
    <Property name='output-variable'>output</Property>
    <Property name='username'>{my_username}</Property>
    <Property name='password'>{my_password}</Property>
    <Property name='password-encoding'>digest</Property>
  </Properties>
  <ClassName>com.google.apigee.callouts.wssecusernametoken.Inject</ClassName>
  <ResourceURL>java://apigee-wssecusernametoken-20231212.jar</ResourceURL>
</JavaCallout>
```

The properties are:

| name                 | description |
| -------------------- | ------------ |
| `source`             | optional. the variable name in which to obtain the source document to sign. Defaults to message.content |
| `output-variable`    | optional. the variable name in which to write the signed XML. Defaults to message.content |
| `username`           | required. the username to use within the UsernameToken |
| `password`           | required. the password to use within the UsernameToken |
| `password-encoding`  | optional. One of: DIGEST, TEXT (case insensitive). Defaults to TEXT. If Digest, then the password is encoded as Base64(SHA1(nonce+created+password)). If TEXT, the password is encoded directly, in plaintext.  |
| `expiry`             | optional. a timespan expression, such as `180s`, `5m`, or `1h`, indicating 180 seconds, 5 minutes, or 1 hour respectively. If included and if it resolves to a timespan greater than zero, the callout will inject a `wsu:Timestamp` element into the document under the WS-Security `Header`, with `wsu:Created` and a `wsu:Expires` child elements. |


See [the example API proxy included here](./bundle) for a working example showing some of the possible policy configurations.


## Example API Proxy Bundle

Import and Deploy the API Proxy to an organization and environment using a tool like [apigeecli](https://github.com/apigee/apigeecli/blob/main/docs/apigeecli.md)
```sh
apigeecli apis create bundle -f apiproxy --name wssec-username -o $ORG --token $TOKEN
apigeecli apis deploy --wait --name wssec-username --ovr --org $ORG --env $ENV --token $TOKEN
```

There are some sample SOAP request documents included in this repo that you can use for demonstrations.

### Invoking the Example proxy:

* Inject a UsernameToken with a plaintext password, into a SOAP 1.1 message

   ```
   apigee=https://my-apigee-endpoint
   curl -i $apigee/wssec-username/inject1  -H content-type:application/xml \
       --data-binary @./sample-data/request1-soap1_1.xml
   ```

* Inject a UsernameToken with a password digest, into a SOAP 1.1 message

   ```
   apigee=https://my-apigee-endpoint
   curl -i $apigee/wssec-username/inject2  -H content-type:application/xml \
       --data-binary @./sample-data/request1-soap1_1.xml
   ```

* Inject a UsernameToken with a password digest, into a SOAP 1.2 message

   ```
   apigee=https://my-apigee-endpoint
   curl -i $apigee/wssec-username/inject2  -H content-type:application/xml \
       --data-binary @./sample-data/request2-soap1_2.xml
   ```

* Inject a UsernameToken with a password digest, and an Timestamp+Expiry, into a SOAP 1.2 message

   ```
   apigee=https://my-apigee-endpoint
   curl -i $apigee/wssec-username/inject3  -H content-type:application/xml \
       --data-binary @./sample-data/request2-soap1_2.xml
   ```

### Before and After Payload Example

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

Then, if you use `password-encoding` of TEXT, and no `expiry`,
the modified payload might look like this:

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

* restriction: Always uses WSSE 1.0. No support for WSSE1.1
* restriction: Cannot inject a SecurityTokenReference
* restriction: Always uses nonce+created+password for the basis of the password hash
