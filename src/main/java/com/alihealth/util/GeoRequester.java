package com.alihealth.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 请求高德地理信息,也可以再抽象HttpRequester。
 * <p>
 * Author: 奉晨
 * date: 2018/6/8 15:58
 */
public class GeoRequester {

    private static Logger logger = Logger.getLogger(GeoRequester.class.toString());

    /**
     * 高德申请的key
     */
    private static final String APPKEY = "3f2a63f42ad5b0351399fa3dae191262";
    private static final String GEO_REQUEST_PREFIX = "http://restapi.amap.com/v3/geocode/geo";


    /**
     * 发送GET请求
     */
    public String sendGet(String url, Map<String, Object> requestParam, Map<String, String> requestHead) {
        return send(url, "GET", requestParam, requestHead);
    }

    /**
     * 发送POST请求
     */
    public String sendPost(String url, Map<String, Object> requestParam, Map<String, String> requestHead) {
        return send(url, "POST", requestParam, requestHead);
    }


    /**
     * @param url          请求URL
     * @param method       请求方法
     * @param requestParam 请求参数 Map<参数名,参数值>
     * @param requestHead  请求头
     * @return
     */
    private String send(String url, String method, Map<String, Object> requestParam, Map<String, String> requestHead) {
        BufferedReader reader = null;
        StringBuffer response = new StringBuffer();
        HttpURLConnection urlConnection = null;

        logger.info("DEBUG:: REQUEST参数: " + requestParam.toString());

        //默认处理高德信息
        if (url == null) {
            url = GEO_REQUEST_PREFIX;
        }

        if (requestParam == null) {
            requestParam = new HashMap<String, Object>();
        }
        if (requestHead == null) {
            requestHead = new HashMap<String, String>();
        }

        try {
            //如果是GET请求， 先根据请求参数，构造get请求链接
            if (method.equalsIgnoreCase("GET") && requestParam.size() > 0) {
                StringBuffer param = new StringBuffer();

                //添加key信息,默认使用地理编码key
                param.append("?key=").append(requestParam.get("key") == null ? APPKEY : requestParam.get("key"));
                for (Map.Entry<String, Object> entry : requestParam.entrySet()) {
                    param.append("&");
                    param.append(entry.getKey()).append("=").append(entry.getValue());
                }
                url += param.toString();
            }

            //获取连接
            URL sendUrl = new URL(url);
            urlConnection = (HttpURLConnection) sendUrl.openConnection();
            urlConnection.setRequestMethod(method);
            urlConnection.setDoOutput(true);
            urlConnection.setDoInput(true);
            urlConnection.setUseCaches(false);

            //处理请求头
            if (requestHead != null) {
                for (Map.Entry<String, String> entry : requestHead.entrySet()) {
                    urlConnection.addRequestProperty(entry.getKey(), entry.getValue());
                }
            }

            //处理POST请求的参数
            if (method.equalsIgnoreCase("POST") && requestParam.size() > 0) {
                StringBuffer param = new StringBuffer();
                param.append("&");
                param.append("key=" + APPKEY);

                for (Map.Entry<String, Object> entry : requestParam.entrySet()) {
                    param.append("&");
                    param.append(entry.getKey()).append("=").append(
                            entry.getValue() == null ? "" : URLEncoder.encode(entry.getValue().toString(), "utf-8"));
                }
                //发送POST请求数据
                urlConnection.getOutputStream().write(param.toString().getBytes("utf-8"));
                urlConnection.getOutputStream().flush();
                urlConnection.getOutputStream().close();
            }

            //读http响应
            reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "utf-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        } catch (Exception e) {
            logger.warning("SEND REQUEST ERROR!");
            throw new RuntimeException("http rerquest error");
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return response.toString();
    }

}