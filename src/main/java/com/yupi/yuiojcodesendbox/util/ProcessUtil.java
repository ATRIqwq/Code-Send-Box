package com.yupi.yuiojcodesendbox.util;

import cn.hutool.core.util.StrUtil;
import com.yupi.yuiojcodesendbox.model.ExecuteMessage;
import org.springframework.util.StopWatch;

import java.io.*;
import java.util.ArrayList;


/**
 * 进程工具类
 */
public class ProcessUtil {

    /**
     *执行进程并获取信息
     * @param runProcess
     * @param opName
     * @return
     */

    public static ExecuteMessage runProcessAndGetMessage(Process runProcess, String opName) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();
            //等待程序执行结束获取错误码
            int exitValue = runProcess.waitFor();
            executeMessage.setExitValue(exitValue);
            //正常退出
            if (exitValue == 0) {
                System.out.println(opName + "成功");
                //分批获取正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                ArrayList<String> outputList = new ArrayList<>();
                String compileOutputLine;
                while ((compileOutputLine = bufferedReader.readLine())!= null){
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StrUtil.join("\n",outputList));
            } else {
                System.out.println(opName + "失败，错误码 " + exitValue);
                //分批获取正常输出
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(runProcess.getInputStream()));
                ArrayList<String> outputList = new ArrayList<>();
                String compileOutputLine;
                //逐行读取
                while ((compileOutputLine = bufferedReader.readLine())!= null){
                    outputList.add(compileOutputLine);
                }
                executeMessage.setMessage(StrUtil.join("\n",outputList));

                //分批获取错误输出
                BufferedReader errorBufferReader = new BufferedReader(new InputStreamReader(runProcess.getErrorStream()));
                ArrayList<String> errorOutputList = new ArrayList<>();
                String errorCompileOutputLine;
                //逐行读取
                while ((errorCompileOutputLine = errorBufferReader.readLine())!= null){
                    errorOutputList.add(errorCompileOutputLine);
                }
                executeMessage.setMessage(StrUtil.join("\n",errorOutputList));
            }
            stopWatch.stop();
            executeMessage.setTime(stopWatch.getLastTaskTimeMillis());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return executeMessage;
    }

    /**
     * 执行交互式进程并获取信息
     *
     * @param runProcess
     * @param args
     * @return
     */
    public static ExecuteMessage runInteractProcessAndGetMessage(Process runProcess, String args) {
        ExecuteMessage executeMessage = new ExecuteMessage();

        try {
            OutputStream outputStream = runProcess.getOutputStream();
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
            String[] s = args.split(" ");
            String join = StrUtil.join("\n", s) + "\n";
            //写入参数
            outputStreamWriter.write(join);
            //相当于回车
            outputStreamWriter.flush();

            //分批获取正常输出
            InputStream inputStream = runProcess.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
            ArrayList<String> outputList = new ArrayList<>();
            StringBuilder compileOutputStringBuilder = new StringBuilder();
            String compileOutputLine;
            //逐行读取
            while ((compileOutputLine = bufferedReader.readLine())!= null){
                compileOutputStringBuilder.append(compileOutputLine);
            }
            executeMessage.setMessage(compileOutputStringBuilder.toString());

            // 记得资源的释放，否则会卡死
            outputStreamWriter.close();
            outputStream.close();
            inputStream.close();
            runProcess.destroy();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return executeMessage;
    }

}
