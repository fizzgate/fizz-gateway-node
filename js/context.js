// 上下文数据结构设计
// 上下文，用于保存客户输入输出和每个步骤的输入与输出结果
var context = {
	// 是否DEBUG模式
	debug:false,
		
	// 各个操作的耗时
	elapsedTimes: [{
		[actionName]: 123, // 操作名称:耗时
	}],
		
    // 客户输入和接口的返回结果
    input: {
        request:{
        	path: "",
            method: "GET/POST",
            headers: {},
            body: {},
            params: {}
        },
        response: { // 聚合接口的响应
            headers: {},
            body: {}
        }
    },

    // 步骤
    step1: {
        requests: {
        	// 接口1
            request1: {
            	// 请求相关参数
                request:{
                    url: "",
                    method: "GET/POST",
                    headers: {},
                    body: {}
                },
                // 根据转换规则转换后的接口响应
                response: {
                    headers: {},
                    body: {}
                }
            },
            // 接口2
            request2: {
                request:{
                    url: "",
                    method: "GET/POST",
                    headers: {},
                    body: {}
                },
                response: {
                headers: {},
                    body: {}
                }
            }
            //...
        },

        // 步骤结果
        result: {}

    }
}