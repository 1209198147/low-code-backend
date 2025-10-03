package com.shikou.aicode.core.builder;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.system.SystemUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Slf4j
public class VueProjectBuilder {

    public static void buildProjectAsync(String projectPath){
        Thread.ofVirtual().name("vue-builder-"+System.currentTimeMillis())
                .start(()->{
                    try {
                        buildProject(projectPath);
                    }catch (Exception e){
                        log.error("异步构建vue项目失败 {}", e.getMessage(), e);
                    }
                });
    }

    public static boolean buildProject(String projectPath){
        File projectDir = new File(projectPath);
        if(!projectDir.exists()){
            log.error("要构建的项目目录不存在 path: {}", projectDir);
            return false;
        }
        File packageJsonFile = new File(projectDir, "package.json");
        if (!packageJsonFile.exists()) {
            log.error("项目目录中没有 package.json 文件：{}", projectPath);
            return false;
        }
        log.info("开始构建vue项目: {}", projectPath);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        if(!executeNpmInstall(projectDir)){
            log.error("执行npm install命令失败: {}", projectPath);
            return false;
        }
        if(!executeNpmBuild(projectDir)){
            log.error("执行npm npm run build命令: {}", projectPath);
            return false;
        }
        File distDir = new File(projectPath, "dist");
        if(!distDir.exists()){
            log.error("构建完成，但未能找到dist文件夹: {}", projectPath);
            return false;
        }
        stopWatch.stop();
        log.info("vue项目构建完成: {} 用时: {}ms", projectPath, stopWatch.getTotalTimeMillis());
        return true;
    }

    private static boolean executeNpmInstall(File projectDir){
        log.info("正在执行npm install命令: {}", projectDir.getAbsolutePath());
        String command = StrUtil.format("{} install", buildCommand("npm"));
        return executeCommand(projectDir, command, 300);
    }

    private static boolean executeNpmBuild(File projectDir){
        log.info("正在执行npm run build命令: {}", projectDir.getAbsolutePath());
        String command = StrUtil.format("{} run build", buildCommand("npm"));
        return executeCommand(projectDir, command, 180);
    }

    private static String buildCommand(String baseCommand) {
        if (isWindows()) {
            return baseCommand + ".cmd";
        }
        return baseCommand;
    }

    private static boolean isWindows(){
        return SystemUtil.getOsInfo().isWindows();
    }

    private static boolean executeCommand(File workingDir, String command, int timeoutSeconds) {
        try {
            log.info("在目录 {} 中执行命令: {}", workingDir.getAbsolutePath(), command);
            Process process = RuntimeUtil.exec(
                    null,
                    workingDir,
                    command.split("\\s+") // 命令分割为数组
            );
            // 等待进程完成，设置超时
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.error("命令执行超时（{}秒），强制终止进程", timeoutSeconds);
                process.destroyForcibly();
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("命令执行成功: {}", command);
                return true;
            } else {
                log.error("命令执行失败，退出码: {}", exitCode);
                return false;
            }
        } catch (Exception e) {
            log.error("执行命令失败: {}, 错误信息: {}", command, e.getMessage());
            return false;
        }
    }
}
