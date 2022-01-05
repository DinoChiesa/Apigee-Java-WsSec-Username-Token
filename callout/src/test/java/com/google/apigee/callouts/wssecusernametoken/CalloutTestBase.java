// Copyright © 2019-2022 Google LLC.
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
import com.apigee.flow.message.Message;
import com.apigee.flow.message.MessageContext;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import mockit.Mock;
import mockit.MockUp;
import org.testng.annotations.BeforeMethod;

public abstract class CalloutTestBase {

  MessageContext msgCtxt;
  InputStream messageContentStream;
  Message message;
  ExecutionContext exeCtxt;

  @BeforeMethod
  public void beforeMethod(Method method) throws Exception {
    String methodName = method.getName();
    String className = method.getDeclaringClass().getName();
    System.out.printf("\n\n==================================================================\n");
    System.out.printf("TEST %s.%s()\n", className, methodName);

    msgCtxt =
        new MockUp<MessageContext>() {
          private Map<String,Object> variables;

          public void $init() {
            variables = new HashMap<String,Object>();
          }

          @Mock()
          public Object getVariable(final String name) {
            if (variables == null) {
              variables = new HashMap<String,Object>();
            }
            return variables.get(name);
          }

          @Mock()
          public boolean setVariable(final String name, final Object value) {
            if (variables == null) {
              variables = new HashMap<String,Object>();
            }
            System.out.printf("setVariable(%s, %s)\n", name, value.toString());
            variables.put(name, value);
            return true;
          }

          @Mock()
          public boolean removeVariable(final String name) {
            if (variables == null) {
              variables = new HashMap<String,Object>();
            }
            if (variables.containsKey(name)) {
              variables.remove(name);
            }
            return true;
          }

          @Mock()
          public Message getMessage() {
            return message;
          }
        }.getMockInstance();

    exeCtxt = new MockUp<ExecutionContext>() {}.getMockInstance();

    message =
        new MockUp<Message>() {
          @Mock()
          public InputStream getContentAsStream() {
            // new ByteArrayInputStream(messageContent.getBytes(StandardCharsets.UTF_8));
            return messageContentStream;
          }
        }.getMockInstance();
  }
}
