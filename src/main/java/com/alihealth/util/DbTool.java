package com.alihealth.util;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 数据操作工具类
 * <p>
 * Author: 奉晨
 * date: 2018/6/9 12:44
 */
public class DbTool {

    private static Logger logger = Logger.getLogger(DbTool.class.toString());
    private static Connection connection = null;

    private String url = "jdbc:mysql://localhost:3306/local_dev?characterEncoding=utf8";
    private String userName = "root";
    private String pass = "123456@";

    /**
     *
     */
    private void initConn() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url, userName, pass);
        } catch (ClassNotFoundException e) {
            logger.info("load mysql driver error");
            e.printStackTrace();
            return;
        } catch (SQLException e) {
            logger.info("MySQL Connection Failed!");
            e.printStackTrace();
            return;
        }
    }

    /**
     * @param sql
     * @return
     */
    public Long insertSingle(String sql) {
        //check parameters
        if (sql == null) {
            return -1L;
        }

        if (connection == null) {
            initConn();
        }
        Statement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            int affectRows = statement.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);

            if (affectRows == 0) {
                throw new SQLException("failed, o rows affected");
            }

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getLong(1);
            } else {
                throw new SQLException("no inserted id returned");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                statement.close();
                return -1L;
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1L;
    }

    /**
     * 批量插入数据，rows为要插入的每行数据， 字段中间使用分号分隔。
     * 默认值操作hospitalgeo表！
     * 字段信息： hosp_geo_id;hosp_code;hosp_desc;format_address;geoinfo
     *
     * @param rows
     * @return
     */
    public boolean insertBatch(List<String> rows) {
        if (rows == null || rows.size() == 0) {
            return false;
        }

        if (connection == null) {
            initConn();
        }

        Statement statement = null;

        StringBuffer sbuf = new StringBuffer("insert into hospitalgeo (hosp_code, hosp_desc, format_address, geoinfo) values ");
        //组装批量插入sql
        for (int i = 0; i < rows.size(); ++i) {
            String[] cols = rows.get(i).split(";");
            if (cols.length == 4) {
                //不接受非空的字段值，如果为空，设置空值
                sbuf.append(Arrays.stream(cols).collect(Collectors.joining("','", "('", "')")));
            } else {
                logger.warning("传入参数有误：" + rows.get(i));
            }

            //最后一个元素不加后缀了
            if (i != rows.size() - 1) {
                sbuf.append(", ");
            }
        }
        String sql = sbuf.append(";").toString();
        logger.info("EXECTE SQL: " + sql);
        //组装完成

        try {
            statement = connection.createStatement();
            int affected = statement.executeUpdate(sql);
            if (affected != rows.size()) {
                return false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            close(null, statement);
        }
        return true;
    }

    private void close(Connection conn, Statement stat) {
        try {
            if (conn != null) {
                conn.close();
            }

            if (stat != null) {
                stat.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    public void setUrl(String url) {
        this.url = url;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }
}
