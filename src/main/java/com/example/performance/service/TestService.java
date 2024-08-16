package com.example.performance.service;

import com.example.performance.entity.Member;
import com.example.performance.mq.MessageProducer;
import com.example.performance.repository.MemberRepository;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@RequiredArgsConstructor
public class TestService {
    private final MessageProducer messageProducer;
    private final MemberRepository memberRepository;
    private final MongoTemplate mongoTemplate;
    private final Gson gson = new Gson();
    private final String queueName = "test";


    @PostConstruct
    public void init() {
        memberRepository.deleteAll();
        String[] names = {"Alice", "Bob", "Charlie", "David", "Eve"};
        String[] emails = {"alice@example.com", "bob@example.com", "charlie@example.com", "david@example.com", "eve@example.com"};
        String[] titles = {"Developer", "Manager", "Designer", "Architect", "Tester"};
        String[] contents = {"Working on project A", "Managing team B", "Designing UI/UX", "Architecting system", "Testing modules"};
        Random random = new Random();

        int size = 100000; // 10만 개의 Member 객체 생성
        int numThreads = 10; // 사용할 쓰레드 수
        int batchSize = size / numThreads; // 각 쓰레드가 처리할 작업 수
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executorService.execute(() -> {
                System.out.println(Thread.currentThread().getName() + " - Started");
                try {
                    BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Member.class);
                    for (int j = 0; j < batchSize; j++) {
                        bulkOps.insert(Member.builder()
                                .name(names[random.nextInt(names.length)])
                                .email(emails[random.nextInt(emails.length)])
                                .title(titles[random.nextInt(titles.length)])
                                .content(contents[random.nextInt(contents.length)])
                                .isSend(false)
                                .build());
                    }
                    bulkOps.execute(); // 각 스레드가 자신의 작업이 끝나면 개별적으로 bulkOps 실행
                } catch (Exception e) {
                    System.err.println(Thread.currentThread().getName() + " - Error: " + e.getMessage());
                }
            });
        }


    }


    public void sendEmailDefault() {
        // 1000개씩 끊어서 저장
        List<Member> memberList = memberRepository.findTop1000ByIsSend(false);
        for(Member member : memberList) {
            sendMessageQueue(member);
        }

    }

    public void sendEmailThread() throws InterruptedException {
        // 1000개씩 끊어서 저장
        List<Member> memberList = memberRepository.findTop1000ByIsSend(false);

        int numThreads = 10; // 사용할 쓰레드 수
        int batchSize = memberList.size() / numThreads; // 각 쓰레드가 처리할 작업 수
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);

        for(int i = 0; i < numThreads; i++) {
            int start = i * batchSize;
            int end = (i == numThreads - 1) ? memberList.size() : (i + 1) * batchSize;
            System.out.println("start = " + start);
            System.out.println("end = " + end);

            List<Member> subList = memberList.subList(start, end);

            executorService.submit(new Runnable() { // execute()를 써도 됨
                @Override
                public void run() {
                    for(Member member : subList) {
                        sendMessageQueue(member);
                    }
                    latch.countDown();
                }
            });
        }
        System.out.println("test");
        // 모든 스레드가 완료될 때까지 대기
        //latch.await();
        System.out.println("done");

        // ExecutorService 종료
        executorService.shutdownNow();
    }


    private synchronized void sendMessageQueue(Member member) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Member.class);
        try {
            messageProducer.sendMessage(queueName, gson.toJson(member));
            member.send();
            bulkOps.upsert(
                    new Query(Criteria.where("_id").is(member.getId())),
                    new Update()
                            .set("name", member.getName())
                            .set("email", member.getEmail())
                            .set("title", member.getTitle())
                            .set("content", member.getContent())
                            .set("isSend", member.getIsSend())
            );
        } catch (Exception e) {
            bulkOps.upsert(
                    new Query(Criteria.where("_id").is(member.getId())),
                    new Update()
                            .set("name", member.getName())
                            .set("email", member.getEmail())
                            .set("title", member.getTitle())
                            .set("content", member.getContent())
                            .set("isSend", false)
            );
        }

        bulkOps.execute();
    }


}
