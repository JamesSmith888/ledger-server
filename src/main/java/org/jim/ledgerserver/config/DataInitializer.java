package org.jim.ledgerserver.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jim.ledgerserver.ledger.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 在应用启动时初始化必要的数据
 * @author James Smith
 */
@Component
@Slf4j
public class DataInitializer implements CommandLineRunner {

    @Resource
    private CategoryService categoryService;

    @Override
    public void run(String... args) throws Exception {
        log.info("开始初始化应用数据...");
        
        // CategoryService 的 @PostConstruct 方法会自动执行
        // 这里可以添加其他初始化逻辑
        
        log.info("应用数据初始化完成");
    }
}