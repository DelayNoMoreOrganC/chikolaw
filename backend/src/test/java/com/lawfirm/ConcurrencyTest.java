package com.lawfirm;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 并发测试
 * 模拟50人同时使用系统
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class ConcurrencyTest {

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private final String BASE_URL = "http://localhost:8080/api";

    /**
     * 测试50人同时登录
     */
    @Test
    public void testConcurrentLogin() throws InterruptedException {
        System.out.println("======================================");
        System.out.println("测试: 50人同时登录");
        System.out.println("======================================");

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int userId = i + 1;
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
                    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(
                        BASE_URL + "/auth/login",
                        request,
                        String.class
                    );

                    long requestEnd = System.currentTimeMillis();
                    long responseTime = requestEnd - requestStart;
                    responseTimes.offer(responseTime);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                        System.out.printf("[登录] 用户#%d 成功 (%dms)%n", userId, responseTime);
                    } else {
                        failCount.incrementAndGet();
                        System.out.printf("[登录] 用户#%d 失败 (HTTP %d)%n",
                            userId, response.getStatusCodeValue());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.printf("[登录] 用户#%d 异常: %s%n", userId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 计算统计数据
        long avgTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .sum() / responseTimes.size();
        long maxTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        long minTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);

        System.out.println("\n======================================");
        System.out.println("测试结果统计");
        System.out.println("======================================");
        System.out.printf("总线程数: %d%n", threadCount);
        System.out.printf("成功: %d%n", successCount.get());
        System.out.printf("失败: %d%n", failCount.get());
        System.out.printf("总耗时: %dms%n", totalTime);
        System.out.printf("平均响应时间: %dms%n", avgTime);
        System.out.printf("最大响应时间: %dms%n", maxTime);
        System.out.printf("最小响应时间: %dms%n", minTime);
        System.out.println("======================================\n");
    }

    /**
     * 测试50人同时查询案件列表
     */
    @Test
    public void testConcurrentCaseQuery() throws InterruptedException {
        System.out.println("======================================");
        System.out.println("测试: 50人同时查询案件列表");
        System.out.println("======================================");

        // 先登录获取token
        String token = login();
        if (token == null) {
            System.err.println("无法获取Token，跳过测试");
            return;
        }

        int threadCount = 50;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        ConcurrentLinkedQueue<Long> responseTimes = new ConcurrentLinkedQueue<>();

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int userId = i + 1;
            executorService.submit(() -> {
                try {
                    long requestStart = System.currentTimeMillis();

                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + token);

                    HttpEntity<String> request = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        BASE_URL + "/cases?page=1&size=20",
                        HttpMethod.GET,
                        request,
                        String.class
                    );

                    long requestEnd = System.currentTimeMillis();
                    long responseTime = requestEnd - requestStart;
                    responseTimes.offer(responseTime);

                    if (response.getStatusCode() == HttpStatus.OK) {
                        successCount.incrementAndGet();
                        System.out.printf("[查询] 用户#%d 成功 (%dms)%n", userId, responseTime);
                    } else {
                        failCount.incrementAndGet();
                        System.out.printf("[查询] 用户#%d 失败 (HTTP %d)%n",
                            userId, response.getStatusCodeValue());
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.printf("[查询] 用户#%d 异常: %s%n", userId, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        // 计算统计数据
        long avgTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .sum() / responseTimes.size();
        long maxTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElse(0);
        long minTime = responseTimes.stream()
            .mapToLong(Long::longValue)
            .min()
            .orElse(0);

        System.out.println("\n======================================");
        System.out.println("测试结果统计");
        System.out.println("======================================");
        System.out.printf("总线程数: %d%n", threadCount);
        System.out.printf("成功: %d%n", successCount.get());
        System.out.printf("失败: %d%n", failCount.get());
        System.out.printf("总耗时: %dms%n", totalTime);
        System.out.printf("平均响应时间: %dms%n", avgTime);
        System.out.printf("最大响应时间: %dms%n", maxTime);
        System.out.printf("最小响应时间: %dms%n", minTime);
        System.out.println("======================================\n");
    }

    /**
     * 测试混合场景（30人登录 + 20人查询）
     */
    @Test
    public void testMixedScenario() throws InterruptedException {
        System.out.println("======================================");
        System.out.println("测试: 混合场景（30人登录 + 20人查询）");
        System.out.println("======================================");

        int loginThreadCount = 30;
        int queryThreadCount = 20;
        int totalThreads = loginThreadCount + queryThreadCount;

        ExecutorService executorService = Executors.newFixedThreadPool(totalThreads);
        CountDownLatch latch = new CountDownLatch(totalThreads);
        AtomicInteger loginSuccess = new AtomicInteger(0);
        AtomicInteger querySuccess = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();

        // 30人登录
        for (int i = 0; i < loginThreadCount; i++) {
            final int userId = i + 1;
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);

                    String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
                    HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

                    ResponseEntity<String> response = restTemplate.postForEntity(
                        BASE_URL + "/auth/login",
                        request,
                        String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK) {
                        loginSuccess.incrementAndGet();
                        System.out.printf("[登录] 用户#%d 成功%n", userId);
                    } else {
                        failCount.incrementAndGet();
                        System.out.printf("[登录] 用户#%d 失败%n", userId);
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.printf("[登录] 用户#%d 异常%n", userId);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 20人查询（使用之前获取的token）
        String token = login();
        for (int i = 0; i < queryThreadCount; i++) {
            final int userId = i + 1;
            executorService.submit(() -> {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.set("Authorization", "Bearer " + token);

                    HttpEntity<String> request = new HttpEntity<>(headers);

                    ResponseEntity<String> response = restTemplate.exchange(
                        BASE_URL + "/cases?page=1&size=20",
                        HttpMethod.GET,
                        request,
                        String.class
                    );

                    if (response.getStatusCode() == HttpStatus.OK) {
                        querySuccess.incrementAndGet();
                        System.out.printf("[查询] 用户#%d 成功%n", userId);
                    } else {
                        failCount.incrementAndGet();
                        System.out.printf("[查询] 用户#%d 失败%n", userId);
                    }
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.out.printf("[查询] 用户#%d 异常%n", userId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(60, TimeUnit.SECONDS);
        executorService.shutdown();

        long endTime = System.currentTimeMillis();
        long totalTime = endTime - startTime;

        System.out.println("\n======================================");
        System.out.println("测试结果统计");
        System.out.println("======================================");
        System.out.printf("登录成功: %d/%d%n", loginSuccess.get(), loginThreadCount);
        System.out.printf("查询成功: %d/%d%n", querySuccess.get(), queryThreadCount);
        System.out.printf("失败: %d%n", failCount.get());
        System.out.printf("总耗时: %dms%n", totalTime);
        System.out.println("======================================\n");
    }

    /**
     * 登录获取Token
     */
    private String login() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String requestBody = "{\"username\":\"admin\",\"password\":\"admin123\"}";
            HttpEntity<String> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                BASE_URL + "/auth/login",
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // 简单的解析token（实际项目中应该使用JSON解析库）
                String body = response.getBody();
                if (body != null && body.contains("\"token\"")) {
                    int start = body.indexOf("\"token\":\"") + 9;
                    int end = body.indexOf("\"", start);
                    return body.substring(start, end);
                }
            }
        } catch (Exception e) {
            System.err.println("登录失败: " + e.getMessage());
        }
        return null;
    }
}
