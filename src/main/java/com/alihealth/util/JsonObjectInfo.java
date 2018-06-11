package com.alihealth.util;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

/**
 * 解析处理高德接口返回的json信息
 * 参考返回信息文档:
 * http://lbs.amap.com/api/webservice/guide/api/georegeo#instructions
 * <p>
 * Author: 奉晨
 * date: 2018/6/10 01:59
 */
public class JsonObjectInfo {


    /**
     * 判断调用是否成功
     */
    public boolean isSuccess(JSONObject jsonObj) {
        if (jsonObj == null) {
            return false;
        }
        return Integer.parseInt(jsonObj.get("status").toString()) == 1;
    }

    /**
     * 返回调用成功的个数
     */
    public int getCount(JSONObject jsonObj) {
        if (jsonObj == null) {
            return -1;
        }

        return Integer.parseInt(jsonObj.get("count").toString());
    }

    /**
     * 从json字符串中获取指定key的信息
     */
    public String getSingleGeoValue(JSONObject jsonObj, String key) {
        if (jsonObj == null || key != null || key.length() == 0) {
            return null;
        }

        JSONArray geoCodes = jsonObj.getJSONArray("geocodes");
        JSONObject codeInfo = geoCodes.getJSONObject(0);

        return codeInfo.get(key).toString();
    }

    /**
     * 处理批量查询的json信息
     *
     * @param jsonObj
     * @return
     */
    public String[] getBatchLocation(JSONObject jsonObj) {
        if (jsonObj == null) {
            return null;
        }

        JSONArray geoCodes = jsonObj.getJSONArray("geocodes");

        int count = getCount(jsonObj);
        String[] res = new String[count];

        if (count == 0) {
            res[0] = "[]";
            return res;
        }

        for (int i = 0; i < count; i++) {
            res[i] = ((JSONObject) geoCodes.get(i)).get("location").toString();
        }
        return res;
    }

    /**
     * 得到接口处理得到的匹配信息
     *
     * @param jsonObj
     * @return
     */
    public String[] getBatchFormatName(JSONObject jsonObj) {
        if (jsonObj == null) {
            return null;
        }

        JSONArray geoCodes = jsonObj.getJSONArray("geocodes");

        int count = getCount(jsonObj);
        String[] res = new String[count];

        if (count == 0) {
            res[0] = "[]";
            return res;
        }

        for (int i = 0; i < count; i++) {
            res[i] = ((JSONObject) geoCodes.get(i)).get("formatted_address").toString();
        }
        return res;
    }

}
