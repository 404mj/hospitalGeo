package com.alihealth.main;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alihealth.util.DbTool;
import com.alihealth.util.GeoRequester;
import com.alihealth.util.JsonObjectInfo;
import com.microsoft.schemas.office.visio.x2012.main.RowType;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * 读Excel内容，
 * 组装任务，
 * 分配线程池处理。
 * <p>
 * Author: 奉晨
 * date: 2018/6/10 18:12
 */
public class GeoworkLeader {
    private static Logger logger = Logger.getLogger(GeoworkLeader.class.toString());

    public static void main(String[] args) {
        GeoworkLeader leader = new GeoworkLeader();
        leader.doMain();
//        leader.patch(null);
    }

    /**
     * 程序入口
     */
    private void doMain() {
        //高德API 最多支持 10 个地址，故。
        ExecutorService pool = Executors.newFixedThreadPool(10);

        //total 3363 lines,10 threads, total process i times.
        int i = (3363 / 10) + 1;
        //阻塞主线程，关闭线程池资源
        CountDownLatch latch = new CountDownLatch(i);

        InputStream inputStream = null;
        Workbook hssfWb = null;
        try {
            //_0525
            inputStream = new FileInputStream("/Users/zsx/Documents/波立维已进药社区名单_0525.xls");
            hssfWb = new HSSFWorkbook(inputStream);
            HSSFSheet sheet = (HSSFSheet) hssfWb.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.rowIterator();

            //已经读了几行数据
            int count = 0;
            List<String> excelRows = null;
            while (rowIterator.hasNext()) {
                //先判断是否到10条数据
                // 每十行数据一组，提交给线程池
                if (count == 10) {
                    //控制并发量，CKQPS_HAS_EXCEEDED_THE_LIMIT
                    Long delay = (new Random().nextInt(5)) * 10L;
                    Thread.sleep(delay);
                    GeoWork task = new GeoWork(latch, excelRows);
                    logger.info("add task: " + excelRows.toString());
                    pool.execute(task);
                    excelRows = null;
                    count = 0;
                }

                HSSFRow row = (HSSFRow) rowIterator.next();
                //两列的值,组成一行数据用一个分号分隔
                String code = row.getCell(0).getStringCellValue().trim();
                String name = row.getCell(1).getStringCellValue().trim();
                //过滤空行
                if (code.length() == 0 && name.length() == 0) {
                    break;
                }
                //pass掉表头
                if (name.equals("医院名称")) {
                    continue;
                }

                if (excelRows == null) {
                    excelRows = new ArrayList<>(10);
                }

                excelRows.add(code + ";" + name);
                count++;
            }
            // 处理读完之后不够十条的情况
            if (count > 0) {
                GeoWork task = new GeoWork(latch, excelRows);
                pool.execute(task);
            }

            latch.await();
            logger.info("done, close resource ...");
            pool.shutdown();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                hssfWb.close();
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param rowinfo
     */
    private void patch(String rowinfo) {
        String[] infoArr = {"ZFNK;浙江省杭州市西湖区未知经销商;NULL", "ZCVD;辽宁省沈阳市未知经销商;NULL", "ZEDV;黑龙江省鹤岗市未知经销商;NULL", "ZFIJ;江苏省盐城市未知经销商;NULL", "ZTAL;新疆维吾尔自治区阿勒泰地区布尔津县未知经销商;NULL", "ZOEL;四川省宜宾市翠屏区未知经销商;NULL",
                "ZDLK;吉林省长春市绿园区未知经销商;NULL", "ZGCX;安徽省合肥市包河区未知经销商;NULL", "ZZFM;江苏省苏州市姑苏区未知经销商;NULL", "ZTAP;新疆维吾尔自治区阿勒泰地区富蕴县未知经销商;NULL"};
        List<String> infoList = Arrays.asList(infoArr);
        DbTool db = new DbTool();
        System.out.println(db.insertBatch(infoList));
    }
}