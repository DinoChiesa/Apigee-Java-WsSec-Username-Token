// Copyright © 2024 Google, LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// All rights reserved.

package com.google.apigee.fakes;

import com.apigee.flow.execution.ExecutionContext;
import com.apigee.flow.message.AsyncContent;
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import com.apigee.flow.message.TransportMessage;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class FakeMessage implements Message {
  Supplier<InputStream> messageContentStreamSupplier;
  InputStream messageContentStream;

  public FakeMessage() {}

  // public FakeMessage(Supplier<InputStream> messageContentStreamSupplier) {
  //   this.messageContentStreamSupplier = messageContentStreamSupplier;
  // }

  public void setContent(InputStream inStream) {
    this.messageContentStream = inStream;
  }

  public void setContent(String content) {
    throw new UnsupportedOperationException();
  }

  public InputStream getContentAsStream() {
    return messageContentStream;
    // return messageContentStreamSupplier.get();
  }

  public String getContent() {
    return new BufferedReader(new InputStreamReader(getContentAsStream()))
        .lines()
        .collect(Collectors.joining("\n"));
  }

  /* ========================================================================= */
  /* Everything below this line is not implemented and not needed in this Fake */

  public Set<String> getHeaderNames() {
    throw new UnsupportedOperationException();
  }

  public String getHeader(String headerName) {
    throw new UnsupportedOperationException();
  }

  public String getHeader(String headerName, int index) {
    throw new UnsupportedOperationException();
  }

  public List<String> getHeaders(String headerName) {
    throw new UnsupportedOperationException();
  }

  public String getHeadersAsString(String headerName) {
    throw new UnsupportedOperationException();
  }

  public Object getHeadersAsObject(String headerName) {
    throw new UnsupportedOperationException();
  }

  public boolean setHeader(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean setHeader(String name, int index, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean setHeaderWithMultipleValues(String name, Collection<String> values) {
    throw new UnsupportedOperationException();
  }

  public boolean removeSharedHeader(String headerName) {
    throw new UnsupportedOperationException();
  }

  public boolean removeHeader(String headerName) {
    throw new UnsupportedOperationException();
  }

  public boolean removeHeader(String headerName, int index) {
    throw new UnsupportedOperationException();
  }

  public AsyncContent setAsyncContent(ExecutionContext ectx, MessageContext mctx) {
    throw new UnsupportedOperationException();
  }

  public void prepareForResponseFlow() {
    throw new UnsupportedOperationException();
  }

  public <T> T getVariable(String name) {
    throw new UnsupportedOperationException();
  }

  public boolean setVariable(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean removeVariable(String name) {
    throw new UnsupportedOperationException();
  }

  public Set<String> getQueryParamNames() {
    throw new UnsupportedOperationException();
  }

  public int getQueryParamsCount() {
    throw new UnsupportedOperationException();
  }

  public String getQueryParam(String queryParamName) {
    throw new UnsupportedOperationException();
  }

  public int getQueryParamValuesCount(String queryParamName) {
    throw new UnsupportedOperationException();
  }

  public String getQueryParam(String queryParamName, int index) {
    throw new UnsupportedOperationException();
  }

  public List<String> getQueryParams(String queryParamName) {
    throw new UnsupportedOperationException();
  }

  public String getQueryParamsAsString(String queryParamName) {
    throw new UnsupportedOperationException();
  }

  public boolean setQueryParam(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  public boolean setQueryParam(String name, int index, Object value) {
    throw new UnsupportedOperationException();
  }

  public void setQueryParamWithMultipleValues(String name, Collection<String> values) {
    throw new UnsupportedOperationException();
  }

  public boolean removeQueryParam(String queryParamName) {
    throw new UnsupportedOperationException();
  }

  public boolean removeQueryParam(String queryParamName, int index) {
    throw new UnsupportedOperationException();
  }

  public TransportMessage getTransportMessage() {
    throw new UnsupportedOperationException();
  }
}
