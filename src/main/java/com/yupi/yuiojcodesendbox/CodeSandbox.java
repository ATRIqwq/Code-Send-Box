package com.yupi.yuiojcodesendbox;


import com.yupi.yuiojcodesendbox.model.ExecuteCodeRequest;
import com.yupi.yuiojcodesendbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {

    /**
     * 执行代码
     *
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
