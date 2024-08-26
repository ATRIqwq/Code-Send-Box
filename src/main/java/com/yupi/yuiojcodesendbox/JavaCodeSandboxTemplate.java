package com.yupi.yuiojcodesendbox;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.dfa.WordTree;
import com.yupi.yuiojcodesendbox.model.ExecuteCodeRequest;
import com.yupi.yuiojcodesendbox.model.ExecuteCodeResponse;
import com.yupi.yuiojcodesendbox.model.ExecuteMessage;
import com.yupi.yuiojcodesendbox.model.JudgeInfo;
import com.yupi.yuiojcodesendbox.util.ProcessUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class JavaCodeSandboxTemplate implements CodeSandbox{

    private static final String GLOBAL_CODE_DIR_NAME = "tmpCode";

    private static final String GLOBAL_JAVA_CLASS_NAME = "Main.java";

    //操作关键字黑名单
    private static final List<String> blackList = Arrays.asList("Files","exec");

    private static final WordTree WORD_TREE;


    //初始化字典树
    static {
        WORD_TREE = new WordTree();
        WORD_TREE.addWords(blackList);
    }

    /**
     * 1. 把用户代码保存为文件
     * @param code
     * @return
     */
    public File saveCodeToFile(String code){
        String userDir = System.getProperty("user.dir");
        String globalCodePathName = userDir + File.separator + GLOBAL_CODE_DIR_NAME;

        if (!FileUtil.exist(globalCodePathName)){
            FileUtil.mkdir(globalCodePathName);
        }
        //用户代码隔离存放
        String  userCodeParentPath = globalCodePathName + File.separator + UUID.randomUUID();
        String  userCodePath = userCodeParentPath + File.separator + GLOBAL_JAVA_CLASS_NAME;
        File userCodeFile = FileUtil.writeString(code, userCodePath, StandardCharsets.UTF_8);
        return userCodeFile;
    }

    /**
     * 2. 编译代码
     * @param userCodeFile
     * @return
     */
    public ExecuteMessage compileFile(File userCodeFile){
        String compileCmd =String.format("javac -encoding utf-8 %s",userCodeFile.getAbsoluteFile());
        try {
            Process compileCmdProcess = Runtime.getRuntime().exec(compileCmd);
            ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(compileCmdProcess, "编译");
            if (executeMessage.getExitValue() != 0){
                throw new RuntimeException("编译错误");
            }
            System.out.println(executeMessage);
            return executeMessage;
        } catch (Exception e) {
//            getErrorResponse(e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 3. 执行代码
     * @param userCodeFile
     * @param inputList
     * @return
     */
    public List<ExecuteMessage> runFile(File userCodeFile,List<String> inputList){
        ArrayList<ExecuteMessage> executeMessageList = new ArrayList<>();
        String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
        for (String inputArgs : inputList) {
            String runCmd = String.format("java -Xmx256m -Dfile.encoding=UTF-8 -cp %s Main %s", userCodeParentPath, inputArgs);
            try {
                Process runProcess = Runtime.getRuntime().exec(runCmd);
                //超时控制
                new Thread(()->{
                    try {
                        TimeUnit.SECONDS.sleep(5);
                        System.out.println("执行超时，中断程序");
                        runProcess.destroy();
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }).start();

                ExecuteMessage executeMessage = ProcessUtil.runProcessAndGetMessage(runProcess, "运行");
                System.out.println(executeMessage);
                executeMessageList.add(executeMessage);
            } catch (Exception e) {
                throw new RuntimeException("执行错误", e);
            }
        }
        return executeMessageList;
    }

    /**
     * 4. 获取输出结果
     * @param executeMessageList
     * @return
     */
    public ExecuteCodeResponse getOutputResponse(List<ExecuteMessage> executeMessageList){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        ArrayList<String> outputList = new ArrayList<>();
        long maxTime = 0;
        for (ExecuteMessage executeMessage : executeMessageList) {
            String errorMessage = executeMessage.getErrorMessage();
            if (StrUtil.isNotBlank(errorMessage)){
                executeCodeResponse.setMessage(errorMessage);
                // 用户提交代码执行错误
                executeCodeResponse.setStatus(3);
                break;
            }
            outputList.add(executeMessage.getMessage());
            Long time = executeMessage.getTime();
            if (time != null) {
                maxTime = Math.max(maxTime, time);
            }
        }
        //正常运行完成
        if (outputList.size() == executeMessageList.size()){
            executeCodeResponse.setStatus(1);
        }
        executeCodeResponse.setOutputList(outputList);
        JudgeInfo judgeInfo = new JudgeInfo();
        judgeInfo.setTime(maxTime);
        executeCodeResponse.setJudgeInfo(judgeInfo);
        return executeCodeResponse;
    }

    /**
     * 5. 清理文件
     * @param userCodeFile
     * @return
     */
    public boolean deleteFile(File userCodeFile){
        if (userCodeFile.getParentFile() != null){
            String userCodeParentPath = userCodeFile.getParentFile().getAbsolutePath();
            boolean del = FileUtil.del(userCodeParentPath);
            System.out.println("删除" + (del?"成功" : "失败"));
            return del;
        }
        return true;
    }

    @Override
    public ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest) {
        List<String> inputList = executeCodeRequest.getInputList();
        String code = executeCodeRequest.getCode();
        String language = executeCodeRequest.getLanguage();

        //校验代码中是否有黑名单中的词
//        FoundWord foundWord = WORD_TREE.matchWord(code);
//        if (ObjectUtil.isNotNull(foundWord)){
//            System.out.println("包含禁止词" + foundWord.getFoundWord());
//            return null;
//        }

        //        1. 把用户的代码保存为文件
        File userCodeFile = saveCodeToFile(code);
        //        2. 编译代码，得到 class 文件
        ExecuteMessage compileFileExecuteMessage = compileFile(userCodeFile);
        System.out.println(compileFileExecuteMessage);
        //        3. 执行代码，得到输出结果
        List<ExecuteMessage> executeMessageList = runFile(userCodeFile, inputList);
        //        4. 收集整理输出结果
        ExecuteCodeResponse outputResponse = getOutputResponse(executeMessageList);
        //        5. 文件清理
        boolean deleted = deleteFile(userCodeFile);
        if (!deleted){
            log.error("deleteFile error,userCodeFilePath = {}",userCodeFile.getAbsoluteFile());
        }
        return outputResponse;
    }

    /**
     * 获取错误响应
     * @param e
     * @return
     */
    private ExecuteCodeResponse getErrorResponse(Throwable e){
        ExecuteCodeResponse executeCodeResponse = new ExecuteCodeResponse();
        executeCodeResponse.setOutputList(new ArrayList<>());
        executeCodeResponse.setMessage(e.getMessage());
        executeCodeResponse.setStatus(2);
        executeCodeResponse.setJudgeInfo(new JudgeInfo());
        return executeCodeResponse;
    }
}
