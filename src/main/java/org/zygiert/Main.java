package org.zygiert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        List<Long> times = new ArrayList<>();
        Parameters parameters = new Parameters(args);
        ProcessBuilder processBuilder = getProcessBuilder(parameters);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        ExecutorService cracTerminationExecutor = Executors.newSingleThreadExecutor();

        for (int i = 0; i < parameters.warmupIterations; i++) {
            Process process = processBuilder.start();
            ProcessWatcher processWatcher = new ProcessWatcher(process.getInputStream());
            executor.execute(processWatcher);
            while (true) {
                if (processWatcher.serverStarted) {
                    System.out.println("Warmup iteration: " + (i + 1));
                    if (parameters.isCRaC) {
                        killCracJavaProcess(cracTerminationExecutor);
                    } else {
                        process.destroy();
                    }
                    break;
                }
            }
        }

        for (int i = 0; i < parameters.measureIterations; i++) {
            long startTime = System.currentTimeMillis();
            Process process = processBuilder.start();
            ProcessWatcher processWatcher = new ProcessWatcher(process.getInputStream());
            executor.execute(processWatcher);
            while (true) {
                if (processWatcher.serverStarted) {
                    long elapsedTime = System.currentTimeMillis() - startTime;
                    times.add(elapsedTime);
                    System.out.println("Elapsed time: " + elapsedTime + " ms");
                    if (parameters.isCRaC) {
                        killCracJavaProcess(cracTerminationExecutor);
                    } else {
                        process.destroy();
                    }
                    break;
                }
            }
        }

        times.forEach(System.out::println);
        System.exit(0);
    }

    private static void killCracJavaProcess(ExecutorService pidFinderExecutor) throws IOException, InterruptedException, ExecutionException {
        Process pidFinderProcess = new ProcessBuilder()
                .command("sh", "-c", "kill -9 `jps | grep 'simple-application.jar' | awk '{print $1}'`")
                .start();
        DefaultProcessListener defaultProcessListener = new DefaultProcessListener(pidFinderProcess.getInputStream());
        pidFinderExecutor.submit(defaultProcessListener).get();
        pidFinderProcess.destroy();
    }

    private static ProcessBuilder getProcessBuilder(Parameters parameters) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.directory(new File(parameters.directory));
        processBuilder.command("sh", "-c", parameters.command);
        return processBuilder;
    }

    private static final class Parameters {
        private final int measureIterations;
        private final int warmupIterations;
        private final String directory;
        private final String command;
        private final boolean isCRaC;

        private Parameters(String[] args) {
            if (args.length != 4) {
                throw new IllegalArgumentException("Invalid number of arguments! You must specify four arguments (measureIterations, warmupIterations, directory, command)!");
            }
            measureIterations = Integer.parseInt(args[0]);
            warmupIterations = Integer.parseInt(args[1]);
            directory = args[2];
            command = args[3];
            isCRaC = command.contains("CRaCRestoreFrom");
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