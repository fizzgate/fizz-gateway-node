/**
 * context 上下文便捷操作函数
 *
 */
var common = {
		/* *********** private function begin *********** */

		/**
		 * 获取上下文中客户端请求对象
		 * @param {*} ctx 上下文 【必填】
		 */
		getInputReq: function (ctx){
		    if(!ctx || !ctx['input'] || !ctx['input']['request']){
		        return {};
		    }
		    return ctx['input']['request']
		},

		/**
		 * 获取上下文步骤中请求接口的请求对象
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名称 【必填】
		 * @param {*} requestName 请求名称 【必填】
		 */
		getStepReq: function (ctx, stepName, requestName){
		    if(!ctx || !stepName || !requestName){
		        return {};
		    }
		    if(!ctx[stepName] || !ctx[stepName]['requests'] || !ctx[stepName]['requests'][requestName] ||
		        !ctx[stepName]['requests'][requestName]['request']){
		        return {};
		    }
		    return ctx[stepName]['requests'][requestName]['request'];
		},

		/**
		 * 获取上下文步骤中请求接口的响应对象
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名称 【必填】
		 * @param {*} requestName 请求名称 【必填】
		 */
		getStepResp: function (ctx, stepName, requestName){
		    if(!ctx || !stepName || !requestName){
		        return {};
		    }
		    if(!ctx[stepName] || !ctx[stepName]['requests'] || !ctx[stepName]['requests'][requestName] ||
		        !ctx[stepName]['requests'][requestName]['response']){
		        return {};
		    }
		    return ctx[stepName]['requests'][requestName]['response'];
		},

		/* *********** private function end *********** */

		/* *********** input begin ************ */

		/**
		 * 获取客户端请求头
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} headerName 请求头字段名 【选填】，不传时返回所有请求头
		 */
		getInputReqHeader: function (ctx, headerName){
		    var req = this.getInputReq(ctx);
		    var headers = req['headers'] || {};
		    return headerName ? headers[headerName] : headers;
		},

		/**
		 * 获取客户端URL请求参数（query string）
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} paramName URL参数名 【选填】，不传时返回所有请求参数
		 */
		getInputReqParam: function (ctx, paramName){
		    var req = this.getInputReq(ctx);
		    var params = req['params'] || {};
		    return paramName ? params[paramName] : params;
		},

		/**
		 * 获取客户端请求体
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} field 字段名 【选填】，不传时返回整个请求体
		 */
		getInputReqBody: function (ctx, field){
		    var req = this.getInputReq(ctx);
		    var body = req['body'] || {};
		    return field ? body[field] : body;
		},

		/**
		 * 获取返回给客户端的响应头
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} headerName 响应头字段名 【选填】，不传时返回所有响应头
		 */
		getInputRespHeader: function (ctx, headerName){
		    var req = this.getInputReq(ctx);
		    var headers = req['headers'] || {};
		    return headerName ? headers[headerName] : headers;
		},

		/**
		 * 获取返回给客户端的响应体
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} field 字段名 【选填】，不传时返回整个响应体
		 */
		getInputRespBody: function (ctx, field){
		    var req = this.getInputReq(ctx);
		    var body = req['body'] || {};
		    return field ? body[field] : body;
		},

		/* *********** input begin ************ */

		/* *********** step request begin ************ */

		/**
		 * 获取步骤中调用的接口的请求头
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名【必填】
		 * @param {*} requestName 请求的接口名 【必填】
		 * @param {*} headerName 请求头字段名 【选填】，不传时返回所有请求头
		 */
		getStepReqHeader: function (ctx, stepName, requestName, headerName){
		    var req = this.getStepReq(ctx, stepName, requestName);
		    var headers = req['headers'] || {};
		    return headerName ? headers[headerName] : headers;
		},

		/**
		 * 获取步骤中调用的接口的URL参数
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名【必填】
		 * @param {*} requestName 请求的接口名 【必填】
		 * @param {*} paramName URL参数名 【选填】，不传时返回所有URL参数
		 */
		getStepReqParam: function (ctx, stepName, requestName, paramName){
		    var req = this.getStepReq(ctx, stepName, requestName);
		    var params = req['params'] || {};
		    return paramName ? params[paramName] : params;
		},

		/**
		 * 获取步骤中调用的接口的请求体
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名【必填】
		 * @param {*} requestName 请求的接口名 【必填】
		 * @param {*} field 字段名 【选填】，不传时返回整个请求体
		 */
		getStepReqBody: function (ctx, stepName, requestName, field){
		    var req = this.getStepReq(ctx, stepName, requestName);
		    var body = req['body'] || {};
		    return field ? body[field] : body;
		},

		/**
		 * 获取步骤中调用的接口的响应头
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名【必填】
		 * @param {*} requestName 请求的接口名 【必填】
		 * @param {*} headerName 响应头字段名 【选填】，不传时返回所有响应头
		 */
		getStepRespHeader: function (ctx, stepName, requestName, headerName){
		    var resp = this.getStepResp(ctx, stepName, requestName);
		    var headers = resp['headers'] || {};
		    return headerName ? headers[headerName] : headers;
		},

		/**
		 * 获取步骤中调用的接口的响应头
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名【必填】
		 * @param {*} requestName 请求的接口名 【必填】
		 * @param {*} field 字段名 【选填】，不传时返回整个响应头
		 */
		getStepRespBody: function (ctx, stepName, requestName, field){
		    var resp = this.getStepResp(ctx, stepName, requestName);
		    var body = resp['body'] || {};
		    return field ? body[field] : body;
		},

		/**
		 * 获取步骤结果
		 * @param {*} ctx 上下文 【必填】
		 * @param {*} stepName 步骤名【必填】
		 * @param {*} field 字段名 【选填】，不传时返回整个步骤结果对象
		 */
		getStepResult: function (ctx, stepName, field){
		    if(!ctx || !stepName || !ctx[stepName]){
		        return {};
		    }
		    var result = ctx[stepName]['result'] || {};
		    return field ? result[field] : result;
		}

		/* *********** step request end ************ */

		,/**
			 ** 乘法函数，用来得到精确的乘法结果
			 ** 说明：javascript的乘法结果会有误差，在两个浮点数相乘的时候会比较明显。这个函数返回较为精确的乘法结果。
			 ** 调用：accMul(arg1,arg2)
			 ** 返回值：arg1乘以 arg2的精确结果
			 **/
			accMul:function (arg1, arg2) {
			var m = 0, s1 = arg1.toString(), s2 = arg2.toString();
			try {
				m += s1.split(".")[1].length;
			} catch (e) {
			}
			try {
				m += s2.split(".")[1].length;
			} catch (e) {
			}
			return Number(s1.replace(".", "")) * Number(s2.replace(".", ""))
				/ Math.pow(10, m);
		},

			/**
			 ** 除法函数，用来得到精确的除法结果
			 ** 说明：javascript的除法结果会有误差，在两个浮点数相除的时候会比较明显。这个函数返回较为精确的除法结果。
			 ** 调用：accDiv(arg1,arg2)
			 ** 返回值：arg1除以arg2的精确结果
			 **/
			accDiv:function (arg1, arg2) {
			var t1 = 0, t2 = 0, r1, r2;
			try {
				t1 = arg1.toString().split(".")[1].length;
			} catch (e) {
			}
			try {
				t2 = arg2.toString().split(".")[1].length;
			} catch (e) {
			}
			with (Math) {
				r1 = Number(arg1.toString().replace(".", ""));
				r2 = Number(arg2.toString().replace(".", ""));
				return (r1 / r2) * pow(10, t2 - t1);
			}
		}


};


