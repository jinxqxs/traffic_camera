package com.jinxqxs.trafficcamera;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.concurrent.TimeUnit;

@Component
public class PythonRunner implements CommandLineRunner {

    private Process process;

    @Override
    public void run(String... args) throws Exception {
        String userDir = System.getProperty("user.dir");
        String scriptPath = userDir + "/python_service/app1.py";
        if (!new java.io.File(scriptPath).exists()) {
            scriptPath = userDir + "/TrafficCamera/python_service/app1.py";
        }

        File scriptFile = new File(scriptPath);
        File logDir = new File(scriptFile.getParentFile(), "logs");
        logDir.mkdirs();

        ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
        pb.directory(scriptFile.getParentFile());
        pb.redirectOutput(ProcessBuilder.Redirect.to(new File(logDir, "yolo_out.log")));
        pb.redirectError(ProcessBuilder.Redirect.to(new File(logDir, "yolo_err.log")));

        System.out.println("Starting Python YOLO Service... (日志: " + logDir.getAbsolutePath() + ")");
        this.process = pb.start();

        // 提升子进程 CPU 优先级
        try {
            long pid = this.process.pid();
            new ProcessBuilder("powershell", "-Command",
                    "(Get-Process -Id " + pid + ").PriorityClass = 'AboveNormal'")
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .start()
                    .waitFor(10, TimeUnit.SECONDS);
            System.out.println("Python started (PID: " + pid + ", AboveNormal)");
        } catch (Exception e) {
            System.out.println("Python started (PID: " + this.process.pid() + ")");
        }
    }

    @PreDestroy
    public void stopPython() {
        if (process != null) {
            process.destroy();
        }
    }
}
