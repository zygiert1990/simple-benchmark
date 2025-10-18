package org.zygiert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        Parameters parameters = new Parameters(args);
        ProcessBuilder processBuilder = getProcessBuilder(parameters);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorService terminationExecutor = Executors.newSingleThreadExecutor();

        for (int i = 0; i < parameters.measureIterations; i++) {
            long startTime = System.currentTimeMillis();
            Process process = processBuilder.start();
            ProcessWatcher processWatcher = new ProcessWatcher(process.getInputStream());
            executor.execute(processWatcher);
            while (true) {
                if (processWatcher.serverStarted) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    System.out.println("Elapsed time: " + elapsedTime + " ms");
                    if (parameters.isNativeImage) {
                        process.destroy();
                        Thread.sleep(1000); // for unknown reason I have to wait here, perhaps for release some resources on OS, otherwise I get strange results
                    } else {
                        killJVMProcess(terminationExecutor);
                    }
                    break;
                }
            }
        }

        System.exit(0);
    }

    private static void killJVMProcess(ExecutorService executor) throws IOException, InterruptedException, ExecutionException {
        Process killProcess = new ProcessBuilder()
                .command("sh", "-c", "kill -9 `jps | grep 'simple-application' | awk '{print $1}'`")
                .start();
        DefaultProcessListener defaultProcessListener = new DefaultProcessListener(killProcess.getInputStream());
        executor.submit(defaultProcessListener).get();
        killProcess.destroy();
    }

    private static ProcessBuilder getProcessBuilder(Parameters parameters) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", parameters.command);
        return processBuilder;
    }

    private static final class Parameters {
        private final int measureIterations;
        private final String command;
        private final boolean isNativeImage;

        private Parameters(String[] args) {
            if (args.length != 2) {
                throw new IllegalArgumentException("Invalid number of arguments! You must specify two arguments (measureIterations, command)!");
            }
            measureIterations = Integer.parseInt(args[0]);
            command = args[1];
            isNativeImage = !command.contains("java");
        }
    }

    private static final class ProcessWatcher implements Runnable {
        private final InputStream inputStream;
        private volatile boolean serverStarted = false;

        private ProcessWatcher(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                    .forEach(line -> {
                                if (line.contains("Server started")) {
                                    serverStarted = true;
                                }
                            }
                    );
        }
    }

    private static final class DefaultProcessListener implements Runnable {
        private final InputStream inputStream;

        private DefaultProcessListener(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(System.out::println);
        }
    }
}