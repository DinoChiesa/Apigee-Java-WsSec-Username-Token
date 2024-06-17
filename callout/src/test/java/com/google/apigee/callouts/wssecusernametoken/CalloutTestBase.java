// Copyright © 2019-2024 Google LLC.
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

import com.google.apigee.fakes.FakeExecutionContext;
import com.google.apigee.fakes.FakeMessage;
import com.google.apigee.fakes.FakeMessageContext;
import java.io.InputStream;
import java.lang.reflect.Method;
import org.testng.annotations.BeforeMethod;

public abstract class CalloutTestBase {

  FakeMessage message;
  FakeMessageContext msgCtxt;
  FakeExecutionContext exeCtxt;

  InputStream messageContentStream;

  @BeforeMethod
  public void beforeMethod(Method method) throws Exception {
    String methodName = method.getName();
    String className = method.getDeclaringClass().getName();
    System.out.printf("\n\n==================================================================\n");
    System.out.printf("TEST %s.%s()\n", className, methodName);

    message = new FakeMessage();
    msgCtxt = new FakeMessageContext(message);
    exeCtxt = new FakeExecutionContext();
  }

  public InputStream getMessageContentStream() {
    return messageContentStream;
  }
}
