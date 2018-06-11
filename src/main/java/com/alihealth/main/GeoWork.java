package com.alihealth.main;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alihealth.util.DbTool;
import com.alihealth.util.GeoRequester;
import com.alihealth.util.JsonObjectInfo;

import javax.naming.Name;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 处理Excel信息
 * 获取经纬度信息
 * 入库
 * <p>
 * Author: 奉晨
 * date: 2018/6/8 16:02
 */
public class GeoWork implements Runnable {

    private static Logger logger = Logger.getLogger(GeoWork.class.toString());
    private static final String[] DICT = {"卫生服务站", "卫生服务中心", "健康服务中心", "医院", "校医院", "门诊部", "卫生院", "健康服务站"};

    //搜索API
    private static final String SEARCH_URL = "http://restapi.amap.com/v3/place/text";
    private static final String DEFAULT_EMPTY = "NULL";
    private static final String EMPTY_LIST = "[]";
    private static final String EMPTY_COMBINATION = ";NULL;NULL";

    //门栓
    private CountDownLatch latch;

    //需要处理的数据
    private List<String> excelRows;

    public GeoWork(CountDownLatch latch, List<String> excelRows) {
        this.latch = latch;
        this.excelRows = excelRows;
    }

    @Override
    public void run() {
        //check patameters...
        if (excelRows == null || excelRows.size() == 0 || latch == null) {
            logger.warning("null parameters...");
            latch.countDown();
            throw new NullPointerException();
        }

        JsonObjectInfo jsonUtil = new JsonObjectInfo();
        GeoRequester geoRequester = new GeoRequester();
        DbTool db = new DbTool();
        String[] locations = null;
        String[] formatNames = null;

        Map<String, Object> req = new HashMap<>(5);
        req.put("poitype", "医疗保健服务");
        req.put("batch", "true");

        StringBuffer buffer = new StringBuffer();
        int i = 0;
        for (String row : excelRows) {
            String name = row.split(";")[1];
            //不处理未知经销商的数据。
            if (name != null && !name.endsWith("未知经销商")) {
                if (i > 0) {
                    buffer.append("|");
                }
                i++;
                buffer.append(name);
            }
        }
        String address = buffer.toString();
        //存在都是未知经销商的情况！
        if (address.length() > 1) {
            req.put("address", buffer.toString());
            String json = geoRequester.sendGet(null, req, null);
            JSONObject jsonObj = JSONObject.parseObject(json);

            if (!jsonUtil.isSuccess(jsonObj)) {
                logger.warning("response error REQUEST: " + req.toString());
                logger.warning("excelRows : " + excelRows.toString());
                logger.warning("ERR INFO: " + json);
                latch.countDown();
                return;
            }

            locations = jsonUtil.getBatchLocation(jsonObj);
            formatNames = jsonUtil.getBatchFormatName(jsonObj);
        }

        //调用成功，处理返回结果(返回顺序和请求顺序一致！)-构造批量入库数据
        //记录可处理的信息个数
        int j = 0;
        List<String> insertRows = new ArrayList<>(10);
        for (String row : excelRows) {
            String name = row.split(";")[1];

            if (!name.endsWith("未知经销商") && locations != null && formatNames != null && j < locations.length) {
                // 请求了接口的有效地址
                row += matchProcess(name, locations[j], formatNames[j]);
                j++;
            } else {
                row += EMPTY_COMBINATION;
            }
            insertRows.add(row);
        }
        logger.info("DEBUG    insertRows:" + insertRows.toString());
        if (!db.insertBatch(insertRows)) {
            logger.warning("insert batch error!");
        }
        latch.countDown();
        logger.finer(Thread.currentThread().getName() + "work fine and done");
    }

    /**
     * 处理高德接口返回的经纬度信息和格式化名称，
     * 做一些匹配度的验证和优化
     *
     * @param location   [] || ***
     * @param formatName [] || ***
     * @return ";接口响应的格式化名称;经纬度信息"
     */
    private String matchProcess(String name, String location, String formatName) {
        boolean noMatch = false;

        if (location.equals(EMPTY_LIST)) {
            //没有查到数据
            noMatch = true;
        } else {
            //查到数据，粗略匹配判断
            for (String e : DICT) {
                if (formatName.contains(e)) {
                    noMatch = false;
                    break;
                }
            }
        }

        //如果粗略不匹配||没查到信息调搜索接口处理 => 使用搜索接口按照地址查询 => formatname也变成地址/name了
        if (noMatch) {
            JSONObject bestPoi = searchAddress(name);
            if (bestPoi != null) {
                String newName = bestPoi.getString("name");
                String newLocation = bestPoi.getString("location");
                newName = (newName.length() > 0 && newName.equals(EMPTY_LIST)) ? DEFAULT_EMPTY : newName;
                newLocation = (newLocation.length() > 0 && newLocation.equals(EMPTY_LIST)) ? DEFAULT_EMPTY : newLocation;
                return ";" + newName + ";" + newLocation;
            }
        }
        return EMPTY_COMBINATION;
    }

    /**
     * 只能单次调用搜索API
     * 返回调用成功的JSONObject
     *
     * @param name
     * @return
     */
    private JSONObject searchAddress(String name) {

        if (null == name || name.length() <= 0) {
            return null;
        }

        GeoRequester requester = new GeoRequester();

        //构造请求参数
        Map<String, Object> req = new HashMap<>();
        req.put("types", "医疗保健服务");
        req.put("children", 1);
        req.put("offset", 3);
        req.put("page", 1);
        req.put("extensions", "base");
        req.put("keywords", name);

        String resjson = requester.sendGet(SEARCH_URL, req, null);
        JSONObject jsonObj = JSONObject.parseObject(resjson);
        JsonObjectInfo jsonUtil = new JsonObjectInfo();
        if (!jsonUtil.isSuccess(jsonObj)) {
            logger.warning("response error REQUEST: " + req.toString());
            logger.warning("SEARCH ERR INFO: " + resjson);
            return null;
        }

        JSONArray pois = jsonObj.getJSONArray("pois");
        return (JSONObject) pois.get(0);//默认第一个是最匹配的！
    }
}
