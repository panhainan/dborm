package com.panhainan.dborm.uitl;

import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author phn
 * @date 2015-4-8
 * @TODO 数据库操作工具类
 */
public class DBUtil {
    static ResourceBundle rbundle = ResourceBundle.getBundle("db");
    private static String driverName = rbundle.getString("driverClassName");
    private static String dbUser = rbundle.getString("username");
    private static String dbPass = rbundle.getString("password");
    private static String dbUrl = rbundle.getString("url");
    private static Logger logger = Logger.getLogger("com.panhainan.dborm.uitl.DBUtil");

    /**
     * @return Connection
     * @date 2015-4-8
     * @TODO 获取数据库连接
     */
    public static Connection getConnection() {
        logger.log(Level.INFO, "Read source db.properties info ... ");
        try {
            logger.log(Level.INFO, "Database connect start ...");
            // 这里使用这种方法已经指定了new出来的Driver是mysql的驱动
            // DriverManager.registerDriver(new Driver());
            Class.forName(driverName).newInstance();
            Connection conn = null;
            conn = DriverManager.getConnection(dbUrl, dbUser, dbPass);
            if (conn != null) {
                logger.log(Level.INFO, "Database connect success : conn = " + conn);
                return conn;
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, e.getMessage());
        }
        logger.log(Level.CONFIG, "Database connect failed !");
        return null;
    }

    public static void closeConnection(Connection conn, PreparedStatement pstm,
                                       ResultSet rs) {
        try { // 捕捉异常
            try {
                if (rs != null) { // 当ResultSet对象的实例rs不为空时
                    rs.close(); // 关闭ResultSet对象
                    logger.log(Level.INFO,"Database rs closed .");
                }
            } finally {
                try {
                    if (pstm != null) { // 当Statement对象的实例stmt不为空时
                        pstm.close(); // 关闭Statement对象
                        logger.log(Level.INFO,"Database pstm closed .");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (conn != null) { // 当Connection对象的实例conn不为空时
                        conn.close(); // 关闭Connection对象
                        logger.log(Level.INFO,"Database connect closed .");
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage());// 输出异常信息
        }
    }

    public static void main(String[] args) {
        Connection conn = getConnection();
        closeConnection(conn, null, null);
    }
}
