package com.shikou.aicode;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.redisson.api.RBitSet;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.BitSet;

@SpringBootTest
public class CheckInTest {
    @Resource
    private RedissonClient redissonClient;

    @Test
    public void test(){
        String key = "test_check_in";
        RBitSet bitSet = redissonClient.getBitSet(key);
        LocalDate localDate = LocalDate.of(2025, 9, 26);
        bitSet.set(localDate.getDayOfYear());
        localDate = LocalDate.of(2025, 9, 28);
        bitSet.set(localDate.getDayOfYear());
        localDate = LocalDate.of(2025, 9, 30);
        bitSet.set(localDate.getDayOfYear());
        localDate = LocalDate.of(2025, 10, 1);
        bitSet.set(localDate.getDayOfYear());
    }

    @Test
    public void getTest(){
        String key = "test_check_in";
        RBitSet bitSet = redissonClient.getBitSet(key);
        BitSet bs = bitSet.asBitSet();
        bs.stream().forEach(i->{
            int year = LocalDate.now().getYear();
            LocalDate day = LocalDate.ofYearDay(year, i);
            System.out.println(day);
        });
    }
}
