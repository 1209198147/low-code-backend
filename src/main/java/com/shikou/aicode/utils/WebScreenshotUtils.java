package com.shikou.aicode.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import com.shikou.aicode.constant.AppConstant;
import com.shikou.aicode.exception.BusinessException;
import com.shikou.aicode.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

@Slf4j
public class WebScreenshotUtils {
    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    public static String saveWebScreenShot(String url){
        if (StringUtils.isBlank(url)) {
            log.error("网页截图失败，url为空");
            return null;
        }
        try{
            String rootPath = AppConstant.TEMP_SCREEN_SHOT_DIR;
            String screenShotDir = rootPath + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(screenShotDir);
            final String SUFFIX = ".png";
            String fileName = RandomUtil.randomString(5);
            String filePath = screenShotDir + File.separator + fileName + SUFFIX;
            webDriver.get(url);
            waitForWebLoad(webDriver);
            byte[] screenshotBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);
            saveImage(screenshotBytes, filePath);
            log.info("原始截图保存成功：{}", filePath);

            String compressedFilePath = screenShotDir + File.separator + fileName + "_compressed" + SUFFIX;
            compressImage(filePath, compressedFilePath);
            log.info("压缩图片保存成功：{}", compressedFilePath);
            FileUtil.del(filePath);
            return compressedFilePath;
        }catch (Exception e){
            log.error("网页截图失败：{}", url, e);
            return null;
        }
    }

    private static void compressImage(String filePath, String compressedFilePath) {
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(filePath),
                    FileUtil.file(compressedFilePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("压缩图片失败：{} -> {}", filePath, compressedFilePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "压缩图片失败");
        }
    }

    private static void saveImage(byte[] screenshotBytes, String filePath) {
        try {
            FileUtil.writeBytes(screenshotBytes, filePath);
        } catch (Exception e) {
            log.error("保存图片失败：{}", filePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存图片失败");
        }
    }

    private static void waitForWebLoad(WebDriver webDriver) {
        try {
            WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
            wait.until(driver -> ((JavascriptExecutor) driver)
                    .executeScript("return document.readyState").
                    equals("complete")
            );
            // 额外等待一段时间，确保动态内容加载完成
            Thread.sleep(2000);
            log.info("页面加载完成");
        } catch (Exception e) {
            log.error("等待页面加载时出现异常，继续执行截图", e);
        }
    }

    private static WebDriver initChromeDriver(int width, int height) {
        try{
            WebDriverManager.chromedriver().setup();
            ChromeOptions options = new ChromeOptions();
            // 无头模式
            options.addArguments("--headless");
            // 禁用GPU（在某些环境下避免问题）
            options.addArguments("--disable-gpu");
            // 禁用沙盒模式（Docker环境需要）
            options.addArguments("--no-sandbox");
            // 禁用开发者shm使用
            options.addArguments("--disable-dev-shm-usage");
            // 设置窗口大小
            options.addArguments(String.format("--window-size=%d,%d", width, height));
            // 禁用扩展
            options.addArguments("--disable-extensions");
            // 设置用户代理
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // 创建驱动
            WebDriver driver = new ChromeDriver(options);
            // 设置页面加载超时
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // 设置隐式等待
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        }catch (Exception e){
            log.error("初始化ChromeDriver失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "初始化 Chrome 浏览器失败");
        }
    }
}
