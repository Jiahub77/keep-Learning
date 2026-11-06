package com.example.bizvision;

import com.example.bizvision.exception.InsufficientStockException;
import com.example.bizvision.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Java 8/17. Uses real MyBatis SQL and Spring transactions against isolated H2.
@SpringBootTest
@AutoConfigureMockMvc
@Import(ProductIntegrationTests.FailureConfiguration.class)
class ProductIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ProductService service;
    @Autowired ObjectMapper json;
    @Autowired FailAfterStockUpdate failure;

    @BeforeEach
    void resetIsolatedDatabase() {
        assertTrue(jdbc.queryForObject("SELECT H2VERSION()", String.class) != null);
        failure.enabled.set(false);
        jdbc.update("DELETE FROM products");
    }

    private long product(int stock) {
        jdbc.update("INSERT INTO products(name, price_in_cents, stock) VALUES ('测试杯',100,?)", stock);
        return jdbc.queryForObject("SELECT MAX(id) FROM products", Long.class);
    }

    private int stock(long id) {
        return jdbc.queryForObject("SELECT stock FROM products WHERE id=?", Integer.class, id);
    }

    @Test
    void crudAndGeneratedId() throws Exception {
        String body = mvc.perform(post("/products").contentType("application/json")
                .content("{\"name\":\"  保温杯  \",\"priceInCents\":100,\"stock\":5}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = Long.parseLong(body);
        mvc.perform(get("/products/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id)).andExpect(jsonPath("$.name").value("保温杯"))
                .andExpect(jsonPath("$.price_in_cents").value(100)).andExpect(jsonPath("$.stock").value(5));
        String update = "{\"name\":\"咖啡杯\",\"priceInCents\":200,\"stock\":8}";
        for (int repeat = 0; repeat < 2; repeat++) {
            mvc.perform(put("/products/{id}", id).contentType("application/json").content(update))
                    .andExpect(status().isOk()).andExpect(content().string(Long.toString(id)));
        }
        assertEquals(8, stock(id));
        assertEquals(200, jdbc.queryForObject("SELECT price_in_cents FROM products WHERE id=?", Integer.class, id).intValue());
        mvc.perform(delete("/products/{id}", id)).andExpect(status().isOk());
        mvc.perform(delete("/products/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/products/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void missingUpdateDoesNotInsert() throws Exception {
        mvc.perform(put("/products/999999").contentType("application/json")
                .content("{\"name\":\"杯\",\"priceInCents\":0,\"stock\":0}"))
                .andExpect(status().isNotFound());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class).intValue());
    }

    @Test
    void paginationOrderingTotalsAndEmptyPages() throws Exception {
        long first = product(1);
        long second = product(2);
        long third = product(3);
        mvc.perform(get("/products?page=1&size=2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.list.length()").value(2)).andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(1)).andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.list[0].id").value(third)).andExpect(jsonPath("$.list[1].id").value(second));
        mvc.perform(get("/products?page=2&size=2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.list.length()").value(1)).andExpect(jsonPath("$.list[0].id").value(first));
        mvc.perform(get("/products?page=3&size=2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.list.length()").value(0)).andExpect(jsonPath("$.total").value(3));
        mvc.perform(get("/products")).andExpect(status().isOk()).andExpect(jsonPath("$.size").value(2));
        mvc.perform(get("/products?page=1&size=100")).andExpect(status().isOk());
    }

    @Test
    void emptyTableAndLargestSupportedOffset() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk())
                .andExpect(jsonPath("$.list.length()").value(0)).andExpect(jsonPath("$.total").value(0));
        mvc.perform(get("/products?page=2147483647&size=1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.list.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=0&size=2", "page=-1&size=2", "page=1&size=0", "page=1&size=-1",
            "page=1&size=101", "page=2147483647&size=100", "page=abc", "page=2147483648"})
    void badPaginationIs400(String query) throws Exception {
        mvc.perform(get("/products?" + query)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\" \"}", "{\"name\":\"杯\",\"stock\":0}",
            "{\"name\":\"杯\",\"priceInCents\":0}", "{\"name\":\"杯\",\"priceInCents\":-1,\"stock\":0}",
            "{\"name\":\"杯\",\"priceInCents\":0,\"stock\":-1}"})
    void invalidCreateNeverInserts(String body) throws Exception {
        mvc.perform(post("/products").contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM products", Integer.class).intValue());
    }

    @Test
    void nameLengthBoundary() throws Exception {
        char[] chars = new char[100];
        java.util.Arrays.fill(chars, '杯');
        String name = new String(chars);
        String body = "{\"name\":" + json.writeValueAsString(name) + ",\"priceInCents\":0,\"stock\":0}";
        mvc.perform(post("/products").contentType("application/json").content(body)).andExpect(status().isCreated());
        body = "{\"name\":" + json.writeValueAsString(name + "杯") + ",\"priceInCents\":0,\"stock\":0}";
        mvc.perform(post("/products").contentType("application/json").content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void stockSuccessInsufficientMissingAndExactZero() throws Exception {
        long id = product(5);
        mvc.perform(post("/products/decrease-stock/{id}", id).contentType("application/json").content("3"))
                .andExpect(status().isOk()).andExpect(content().string(Long.toString(id)));
        assertEquals(2, stock(id));
        mvc.perform(post("/products/decrease-stock/{id}", id).contentType("application/json").content("3"))
                .andExpect(status().isConflict()).andExpect(content().string("库存不足"));
        assertEquals(2, stock(id));
        mvc.perform(post("/products/decrease-stock/999999").contentType("application/json").content("1"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/products/decrease-stock/{id}", id).contentType("application/json").content("2"))
                .andExpect(status().isOk());
        assertEquals(0, stock(id));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "{}", "{\"quantity\":3}", "2147483648", "garbage", ""})
    void invalidQuantityNeverChangesStock(String body) throws Exception {
        long id = product(5);
        mvc.perform(post("/products/decrease-stock/{id}", id).contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
        assertEquals(5, stock(id));
    }

    @Test
    void nullQuantityMustNotDeductStock() throws Exception {
        long id = product(5);
        mvc.perform(post("/products/decrease-stock/{id}", id).contentType("application/json").content("null"))
                .andExpect(status().isBadRequest());
        assertEquals(5, stock(id));
    }

    @Test
    void rollbackAfterActualSqlUpdate() {
        long id = product(97);
        failure.enabled.set(true);
        RuntimeException error = assertThrows(RuntimeException.class, () -> service.decreaseStock(id, 3));
        assertTrue(error.getMessage().contains("test failure after SQL"));
        assertFalse(failure.enabled.get()); // interceptor reached only after SQL executed successfully
        assertEquals(97, stock(id));
        assertTrue(service.decreaseStock(id, 3).isPresent());
        assertEquals(94, stock(id));
    }

    @Test
    void concurrentDecrementsDoNotOversell() throws Exception {
        long id = product(5);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 10; i++) {
                results.add(pool.submit(() -> {
                    start.await();
                    try { return service.decreaseStock(id, 1).isPresent(); }
                    catch (InsufficientStockException e) { return false; }
                }));
            }
            start.countDown();
            int success = 0;
            for (Future<Boolean> result : results) if (result.get(20, TimeUnit.SECONDS)) success++;
            assertEquals(5, success);
            assertEquals(0, stock(id));
        } finally {
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @TestConfiguration
    static class FailureConfiguration {
        @Bean FailAfterStockUpdate failAfterStockUpdate() { return new FailAfterStockUpdate(); }
    }

    // Test-only failure injection AFTER the real MyBatis UPDATE, inside the real service transaction.
    @Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
    static class FailAfterStockUpdate implements Interceptor {
        final AtomicBoolean enabled = new AtomicBoolean();
        @Override public Object intercept(Invocation invocation) throws Throwable {
            Object result = invocation.proceed();
            MappedStatement statement = (MappedStatement) invocation.getArgs()[0];
            if (statement.getId().endsWith(".decreaseStock") && enabled.compareAndSet(true, false)) {
                throw new RuntimeException("test failure after SQL");
            }
            return result;
        }
    }
}
