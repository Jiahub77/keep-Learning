package com.example.learning;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppTest {
    @Test
    void addsMoney() {
        assertEquals(12900, App.addMoney(9900, 3000));
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> App.addMoney(-1, 3000));
    }
}
