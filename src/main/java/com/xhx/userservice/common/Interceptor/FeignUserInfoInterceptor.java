package com.xhx.userservice.common.Interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.seata.core.context.RootContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 此工具类由AI生成
 * @author ChatGPT
 */
@Component
public class FeignUserInfoInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }

        HttpServletRequest request = ((ServletRequestAttributes) attrs).getRequest();

        // 获取请求头中的用户信息
        String userId = request.getHeader("user-Info");
        String role = request.getHeader("user-Role");
        String ip = request.getHeader("user-Ip");

        if (userId != null) {
            template.header("user-Info", userId);
        }
        if (role != null) {
            template.header("user-Role", role);
        }
        if (ip != null) {
            template.header("user-Ip", ip);
        }

        String xid = RootContext.getXID();
        if (xid != null) {
            template.header(RootContext.KEY_XID, xid);
        }
    }
}
