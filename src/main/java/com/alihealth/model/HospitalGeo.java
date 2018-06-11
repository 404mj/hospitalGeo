package com.alihealth.model;

/**
 * Author: 奉晨
 * date: 2018/6/9 14:15
 */
public class HospitalGeo {

    /**
     * CREATE TABLE `hospitalgeo` (
     * `hosp_geo_id` int NOT NULL auto_increment,
     * `hosp_code` varchar(25) DEFAULT NULL,
     * `hosp_desc` varchar(255) DEFAULT NULL,
     * `geoinfo` varchar(255) DEFAULT NULL,
     * PRIMARY KEY (`hosp_geo_id`)
     * ) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT = 1;
     */

    private String hospitalCode;
    private String hospitalDesc;
    private String geoInfo;

    public String getHospitalCode() {
        return hospitalCode;
    }

    public void setHospitalCode(String hospitalCode) {
        this.hospitalCode = hospitalCode;
    }

    public String getHospitalDesc() {
        return hospitalDesc;
    }

    public void setHospitalDesc(String hospitalDesc) {
        this.hospitalDesc = hospitalDesc;
    }

    public String getGeoInfo() {
        return geoInfo;
    }

    public void setGeoInfo(String geoInfo) {
        this.geoInfo = geoInfo;
    }
}
