/*
MIT License

Copyright (c) 2018 liuzhengyang

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */
package we.proxy.grpc.client.core;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static org.apache.commons.lang3.StringUtils.isNotBlank;



/**
 * @author zhangjikai
 * Created on 2018-12-16
 */

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GrpcMethodDefinition {
    private String packageName;
    private String serviceName;
    private String methodName;

    public String getFullServiceName() {
        if (isNotBlank(packageName)) {
            return packageName + "." + serviceName;
        }
        return serviceName;
    }

    public String getFullMethodName() {
        if (isNotBlank(packageName)) {
            return packageName + "." + serviceName + "/" + methodName;
        }
        return serviceName + "/" + methodName;
    }
}
