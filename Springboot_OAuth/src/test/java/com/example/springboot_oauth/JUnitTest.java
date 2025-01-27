package com.example.springboot_oauth;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class JUnitTest {
    @DisplayName("1+2는 3이다")
    @Test
    public void test1() {
        int a = 1;
        int b = 3;
        int sum = 3;

        Assertions.assertEquals(sum, a + b);
    }
}
