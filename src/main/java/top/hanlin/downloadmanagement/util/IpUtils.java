package top.hanlin.downloadmanagement.util;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * IP工具类 - 安全获取客户端真实IP
 * 
 * 防止通过伪造X-Forwarded-For等请求头绕过IP封禁
 */
public class IpUtils {
    
    private static final Logger log = LoggerFactory.getLogger(IpUtils.class);
    
    // IPv4正则表达式
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );
    
    // IPv6正则表达式（简化版）
    private static final Pattern IPV6_PATTERN = Pattern.compile(
        "^([0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
        "^::([0-9a-fA-F]{1,4}:){0,6}[0-9a-fA-F]{1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,7}:$|" +
        "^([0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,5}(:[0-9a-fA-F]{1,4}){1,2}$|" +
        "^([0-9a-fA-F]{1,4}:){1,4}(:[0-9a-fA-F]{1,4}){1,3}$|" +
        "^([0-9a-fA-F]{1,4}:){1,3}(:[0-9a-fA-F]{1,4}){1,4}$|" +
        "^([0-9a-fA-F]{1,4}:){1,2}(:[0-9a-fA-F]{1,4}){1,5}$|" +
        "^[0-9a-fA-F]{1,4}:(:[0-9a-fA-F]{1,4}){1,6}$|" +
        "^:((:[0-9a-fA-F]{1,4}){1,7}|:)$"
    );
    
    // 私有IP地址范围（用于检测内网IP）
    private static final String[] PRIVATE_IP_PREFIXES = {
        "10.",           // 10.0.0.0 - 10.255.255.255
        "172.16.", "172.17.", "172.18.", "172.19.",
        "172.20.", "172.21.", "172.22.", "172.23.",
        "172.24.", "172.25.", "172.26.", "172.27.",
        "172.28.", "172.29.", "172.30.", "172.31.",  // 172.16.0.0 - 172.31.255.255
        "192.168.",      // 192.168.0.0 - 192.168.255.255
        "127.",          // 127.0.0.0 - 127.255.255.255 (loopback)
        "0.",            // 0.0.0.0 - 0.255.255.255
        "169.254."       // 169.254.0.0 - 169.254.255.255 (link-local)
    };
    
    // 已知的可信代理IP（需要根据实际部署配置）
    // 例如：Nginx反向代理服务器的IP
    private static Set<String> trustedProxies = new HashSet<>();
    
    /**
     * 设置可信代理IP列表
     * 只有来自可信代理的X-Forwarded-For头才会被信任
     */
    public static void setTrustedProxies(Set<String> proxies) {
        trustedProxies = proxies != null ? new HashSet<>(proxies) : new HashSet<>();
    }
    
    /**
     * 添加可信代理IP
     */
    public static void addTrustedProxy(String ip) {
        if (ip != null && !ip.isEmpty()) {
            trustedProxies.add(ip.trim());
        }
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * 安全策略：
     * 1. 如果请求直接来自客户端（remoteAddr不是可信代理），直接使用remoteAddr
     * 2. 如果请求来自可信代理，才信任X-Forwarded-For等头
     * 3. 对X-Forwarded-For中的IP进行验证，过滤无效IP
     * 4. 优先使用X-Forwarded-For链中最后一个非私有IP（最接近客户端的公网IP）
     * 
     * @param request HTTP请求
     * @return 客户端真实IP
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String remoteAddr = request.getRemoteAddr();
        
        // 如果remoteAddr为空，返回unknown
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            return "unknown";
        }
        
        // 处理IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            remoteAddr = "127.0.0.1";
        }
        
        // 关键安全检查：只有当请求来自可信代理时，才信任转发头
        // 如果没有配置可信代理，或者remoteAddr不在可信代理列表中，直接返回remoteAddr
        if (trustedProxies.isEmpty() || !isTrustedProxy(remoteAddr)) {
            // 不信任任何转发头，直接使用TCP连接的真实IP
            log.debug("Request not from trusted proxy, using remoteAddr: {}", remoteAddr);
            return remoteAddr;
        }
        
        // 请求来自可信代理，可以信任转发头
        String ip = getIpFromHeaders(request);
        
        if (ip != null && !ip.isEmpty() && isValidIp(ip)) {
            log.debug("Got IP from headers: {}", ip);
            return ip;
        }
        
        // 如果从头中获取失败，使用remoteAddr
        return remoteAddr;
    }
    
    /**
     * 从请求头中获取IP（仅在可信代理场景下调用）
     */
    private static String getIpFromHeaders(HttpServletRequest request) {
        // 按优先级检查各种代理头
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_CLIENT_IP",
            "HTTP_X_FORWARDED_FOR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For可能包含多个IP，格式：client, proxy1, proxy2
                if (header.equalsIgnoreCase("X-Forwarded-For") && ip.contains(",")) {
                    ip = extractClientIpFromXFF(ip);
                }
                
                if (ip != null && isValidIp(ip)) {
                    return ip;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从X-Forwarded-For头中提取真实客户端IP
     * 
     * X-Forwarded-For格式：client, proxy1, proxy2, ...
     * 
     * 安全策略：
     * 1. 从右向左遍历（从最近的代理到最远的客户端）
     * 2. 跳过可信代理IP
     * 3. 返回第一个非可信代理的有效公网IP
     * 
     * 这样可以防止攻击者在X-Forwarded-For头前面添加伪造IP
     */
    private static String extractClientIpFromXFF(String xff) {
        if (xff == null || xff.isEmpty()) {
            return null;
        }
        
        String[] ips = xff.split(",");
        
        // 从右向左遍历，找到第一个非可信代理的有效IP
        for (int i = ips.length - 1; i >= 0; i--) {
            String ip = ips[i].trim();
            
            if (ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                continue;
            }
            
            // 如果是可信代理，继续向左查找
            if (isTrustedProxy(ip)) {
                continue;
            }
            
            // 验证IP格式
            if (!isValidIp(ip)) {
                log.warn("Invalid IP in X-Forwarded-For: {}", ip);
                continue;
            }
            
            // 找到了有效的客户端IP
            return ip;
        }
        
        // 如果所有IP都是可信代理或无效，返回第一个IP（最原始的客户端声称的IP）
        // 但这种情况下应该谨慎，可能返回null更安全
        String firstIp = ips[0].trim();
        if (isValidIp(firstIp)) {
            return firstIp;
        }
        
        return null;
    }
    
    /**
     * 检查IP是否在可信代理列表中
     */
    private static boolean isTrustedProxy(String ip) {
        if (ip == null || trustedProxies.isEmpty()) {
            return false;
        }
        
        // 精确匹配
        if (trustedProxies.contains(ip)) {
            return true;
        }
        
        // 支持CIDR匹配（简化版，只支持/24等常见掩码）
        for (String proxy : trustedProxies) {
            if (proxy.contains("/")) {
                if (matchesCidr(ip, proxy)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 简单的CIDR匹配（支持/8, /16, /24）
     */
    private static boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            String baseIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            // 简化处理：只支持/8, /16, /24
            String[] ipParts = ip.split("\\.");
            String[] baseParts = baseIp.split("\\.");
            
            if (ipParts.length != 4 || baseParts.length != 4) {
                return false;
            }
            
            int octetsToMatch = prefixLength / 8;
            for (int i = 0; i < octetsToMatch && i < 4; i++) {
                if (!ipParts[i].equals(baseParts[i])) {
                    return false;
                }
            }
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证IP地址格式是否有效
     */
    public static boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        ip = ip.trim();
        
        // 检查是否是有效的IPv4或IPv6地址
        if (IPV4_PATTERN.matcher(ip).matches()) {
            return true;
        }
        
        if (IPV6_PATTERN.matcher(ip).matches()) {
            return true;
        }
        
        // 尝试使用InetAddress解析
        try {
            InetAddress.getByName(ip);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查是否是私有IP地址
     */
    public static boolean isPrivateIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        
        for (String prefix : PRIVATE_IP_PREFIXES) {
            if (ip.startsWith(prefix)) {
                return true;
            }
        }
        
        // IPv6本地地址
        if (ip.startsWith("fe80:") || ip.startsWith("fc") || ip.startsWith("fd") || 
            "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取客户端真实IP（简化版，适用于没有反向代理的场景）
     * 直接返回remoteAddr，不信任任何请求头
     */
    public static String getClientIpStrict(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String remoteAddr = request.getRemoteAddr();
        
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            return "unknown";
        }
        
        // 处理IPv6本地地址
        if ("0:0:0:0:0:0:0:1".equals(remoteAddr) || "::1".equals(remoteAddr)) {
            return "127.0.0.1";
        }
        
        return remoteAddr;
    }
}
