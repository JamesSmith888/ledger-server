package org.jim.ledgerserver.common.interceptor;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 请求体缓存过滤器
 * 为/mcp路径的请求缓存请求体，以便后续多次读取
 *
 * @author James Smith
 */
@Slf4j
@Component
@Order(1) // 确保在其他过滤器之前执行
public class CachedBodyFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        
        // 只对/mcp路径的请求进行缓存处理
        if (httpRequest.getRequestURI().startsWith("/mcp")) {
            // 包装请求以缓存请求体
            CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
            chain.doFilter(cachedRequest, response);
        } else {
            // 其他请求直接放行
            chain.doFilter(request, response);
        }
    }
}