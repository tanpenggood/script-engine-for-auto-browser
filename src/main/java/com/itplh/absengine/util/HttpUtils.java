package com.itplh.absengine.util;

import com.itplh.absengine.script.DelayVariable;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HttpUtils {

    public static final String HTTP_SCHEMA = "http://";
    public static final String HTTPS_SCHEMA = "https://";
    public static final String ABSOLUTE_SCHEMA = "//";
    public static final String METHOD_GET = "get";
    public static final String METHOD_POST = "post";

    private static HashMap<String, String> headers = new HashMap<>();

    static {
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        headers.put("Cache-Control", "max-age-0");
        headers.put("Connection", "keep-alive");
        headers.put("User-Agent", "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko)");
    }

    public static Optional<Document> requestGet(String url, DelayVariable delayVariable) {
        Connection connection = getConnection(url);
        // The first time request
        Optional<Document> documentOptional = requestGet(connection, 1, delayVariable);
        // The second time request, if fail
        if (hasNotResponse(connection)) {
            documentOptional = requestGet(connection, 2, delayVariable);
        }
        // The third time request, if fail
        if (hasNotResponse(connection)) {
            documentOptional = requestGet(connection, 3, delayVariable);
        }
        // The fourth time request, if fail
        if (hasNotResponse(connection)) {
            documentOptional = requestGet(connection, 4, delayVariable);
        }
        return documentOptional;
    }

    public static Element clickLink(Element element, String linkText, DelayVariable delayVariable) {
        if (element == null || StringUtils.isBlank(linkText)) {
            return element;
        }
        String url = LinkAndFormUtils.resolveLinkURL(element, linkText);
        if (StringUtils.hasText(url)) {
            element = requestGet(url, delayVariable).orElse(null);
        }
        return element;
    }

    public static Element formSubmit(Element element, DelayVariable delayVariable, String... value) {
        if (element == null || value == null || value.length == 0) {
            return element;
        }
        String url = LinkAndFormUtils.resolveFormURL(element);
        if (StringUtils.hasText(url)) {
            if (LinkAndFormUtils.isGetForm(element)) {
                String parameter = LinkAndFormUtils.buildGetParameters(element, value);
                url += parameter;
                element = requestGet(url, delayVariable).orElse(null);
            }
            if (LinkAndFormUtils.isPostForm(element)) {
                // TODO
            }
        }
        return element;
    }

    private static Connection getConnection(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        if (url.startsWith(HTTP_SCHEMA) || url.startsWith(HTTPS_SCHEMA)) {
        } else if (url.startsWith(ABSOLUTE_SCHEMA)) {
            url = HTTP_SCHEMA + url.substring(ABSOLUTE_SCHEMA.length());
        } else {
            throw new RuntimeException();
        }
        return Jsoup.connect(url).headers(headers).timeout(15000);
    }

    private static Optional<Document> requestGet(Connection connection, int requestTime, DelayVariable delayVariable) {
        switch (requestTime) {
            case 1:
                DelayUtils.delay(delayVariable);
                break;
            default:
                log.warn("After waiting for 21 seconds, retry the {}th time request.", requestTime);
                DelayUtils.delay(21L, TimeUnit.SECONDS);
                break;
        }
        return Optional.ofNullable(connection)
                .map(con -> {
                    try {
                        return con.get();
                    } catch (IOException e) {
                    }
                    return null;
                });
    }

    private static boolean hasNotResponse(Connection connection) {
        AssertUtils.assertNotNull(connection, "connection is required.");
        try {
            Connection.Response response = connection.response();
            return response == null;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

}
