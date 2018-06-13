package com.alihealth.main;


import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alihealth.util.DbTool;
import com.alihealth.util.GeoRequester;
import com.alihealth.util.JsonObjectInfo;

import javax.naming.Name;
import java.sql.SQLException;
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
            //只处理包含信息的数据。
            if (name != null && isDescMeaningful(name)) {
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
                logger.warning("RESPONSE ERROR REQUEST: " + req.toString());
                logger.warning("EXCEL ROWS : " + excelRows.toString());
                logger.warning("RESPONSE ERROR INFO: " + json);
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
            String desc = row.split(";")[1];

            if (isDescMeaningful(desc) && locations != null && formatNames != null && j < locations.length) {
                // 请求了接口的有效地址
                row += matchProcess(desc, locations[j], formatNames[j]);
                j++;
            } else {
                row += EMPTY_COMBINATION;
            }
            insertRows.add(row);
        }
        logger.info("DEBUG INSERT ROWS:" + insertRows.toString());

        if (!db.insertBatch(insertRows)) {
            logger.warning("INSERT BATCH ERROR!");
        }
        latch.countDown();
        logger.info(Thread.currentThread().getName() + "work fine and done");
    }

    /**
     * 处理高德接口返回的经纬度信息和格式化名称，
     * 做一些匹配度的验证和优化
     *
     * @param desc       原始文件中的描述名称
     * @param location   [] || ***
     * @param formatName [] || ***  请求响应中匹配的名称
     * @return ";接口响应的格式化名称;经纬度信息"
     */
    private String matchProcess(String desc, String location, String formatName) {
        boolean match = false;

        if (!location.equals(EMPTY_LIST)) {
            //查到数据，粗略匹配判断
            for (String e : DICT) {
                if (formatName.contains(e)) {
                    match = true;
                    break;
                }
            }
        }

        //如果粗略不匹配||没查到信息调搜索接口处理 => 使用搜索接口按照地址查询 => formatname也变成地址/name了
        if (!match) {
            JSONObject bestPoi = searchAddress(desc);
            if (bestPoi != null) {
                String newDesc = bestPoi.getString("name");
                String newLocation = bestPoi.getString("location");
                newDesc = (newDesc.length() > 0 && newDesc.equals(EMPTY_LIST)) ? DEFAULT_EMPTY : newDesc;
                newLocation = (newLocation.length() > 0 && newLocation.equals(EMPTY_LIST)) ? DEFAULT_EMPTY : newLocation;
                return ";" + newDesc + ";" + newLocation;
            } else {
                logger.warning("EMPTY SEARCH: " + desc + "--" + location + "--" + formatName);
            }
        }
        //匹配 || 不匹配但是搜索失败
        return ";" + formatName + ";" + location;
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

        // {"status":"0","info":"CKQPS_HAS_EXCEEDED_THE_LIMIT","infocode":"10020"}
        try {
            TimeUnit.MILLISECONDS.sleep(new Random().nextInt(9) * 10L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String resjson = requester.sendGet(SEARCH_URL, req, null);
        JSONObject jsonObj = JSONObject.parseObject(resjson);
        JsonObjectInfo jsonUtil = new JsonObjectInfo();
        if (!jsonUtil.isSuccess(jsonObj)) {
            logger.warning("RESPONSE ERROR REQUEST: " + req.toString());
            logger.warning("SEARCH ERR INFO: " + resjson);
            return null;
        }

        JSONArray pois = jsonObj.getJSONArray("pois");

        if (pois.size() == 0) {
            return null;
        }
        return (JSONObject) pois.get(0);//默认第一个是最匹配的！
    }

    /**
     * 判断机构名称描述是否包含有意义信息
     *
     * @param desc
     * @return
     */
    private boolean isDescMeaningful(String desc) {
        boolean flag = true;

        if (desc.endsWith("未知经销商")) {
            flag = false;
        }

        if (desc.endsWith("其他")) {
            flag = false;
        }

        return flag;
    }
}
