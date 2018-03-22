package com.panhainan.dborm.repository;

import com.panhainan.dborm.uitl.DBUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

class Repository<T> {

    private Logger logger = Logger.getLogger(this.toString());

    /**
     * @param insertSql
     * @return 插入的数据的id
     * 执行插入语句
     */
    int executeInsert(String insertSql) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
        int insertId = 0;
        try {
            pstm = conn.prepareStatement(insertSql,
                    PreparedStatement.RETURN_GENERATED_KEYS);
            pstm.executeUpdate();
            rs = pstm.getGeneratedKeys();
            while (rs.next()) {
                insertId = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } finally {
            DBUtil.closeConnection(conn, pstm, rs);
        }
        return insertId;
    }

    /**
     * @param updateSql
     * @return 数据库受影响的行数
     * 执行更新语句或者删除语句
     */
    int executeUpdateAndDelete(String updateSql) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm = null;
        int updateResult = 0;
        try {
            pstm = conn.prepareStatement(updateSql);
            updateResult = pstm.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } finally {
            DBUtil.closeConnection(conn, pstm, null);
        }
        return updateResult;
    }

    /**
     * @param sql
     * @param obj
     * @return T
     * 通过数据库标识字段id查找
     */
    T executeGet(String sql, Class<T> obj) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm;
        ResultSet rs;
        T o = null;
        try {
            pstm = conn.prepareStatement(sql);
            rs = pstm.executeQuery();
            o = obj.newInstance();
            if (rs.next()) {
                o = setTableToEntity(obj, rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        }
        return o;
    }

    /**
     * @param sql
     * @param c
     * @return List
     * 根据参数params获取list
     */
    List<T> executeList(String sql, Class<T> c) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm;
        ResultSet rs;
        List<T> listObjects = null;
        T o;
        try {
            pstm = conn.prepareStatement(sql);
            rs = pstm.executeQuery();
            listObjects = new ArrayList<T>();
            while (rs.next()) {
                o = c.newInstance();
                o = setTableToEntity(c, rs);
                listObjects.add(o);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        }
        return listObjects;
    }

    /**
     * @param sql
     * @return
     * @date 2015-4-11
     * 获取总数
     */
    protected int getCountRow(String sql) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
        int countRow = 0;
        try {
            pstm = conn.prepareStatement(sql);
            rs = pstm.executeQuery();
            if (rs.next()) {
                countRow = rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        }

        return countRow;
    }

    /**
     * @param c
     * @param rs
     * @return T
     * @date 2015-4-10
     * 将数据库中查询出来的结果集ResultSet转化为实体
     */
    private T setTableToEntity(Class<T> c, ResultSet rs) {
        T o = null;
        try {
            o = c.newInstance();
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            String[] columnName = new String[columnCount];
            String[] columnClassName = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                columnName[i] = rsmd.getColumnName(i + 1);
                StringBuilder fieldName = new StringBuilder(columnName[i]);
                //当属性名包含下划线时，将下划线去掉，同时将下划线后面的第一个字母大写，形成驼峰命名法。
                while (fieldName.toString().contains("_")) {
                    int index = fieldName.indexOf("_");
                    fieldName.replace(index, index + 2, String.valueOf(Character.toUpperCase(fieldName.charAt(index + 1))));
                }
                Class<?> paramType = c.getDeclaredField(fieldName.toString()).getType();
                fieldName.setCharAt(0, Character.toUpperCase(fieldName.charAt(0)));
                String attributeSetName = "set" + fieldName.toString();
                Method md = c.getMethod(attributeSetName, paramType);
                columnClassName[i] = rsmd.getColumnClassName(i + 1);
                if ("java.lang.Integer".equals(columnClassName[i])) {
                    Integer i1 = rs.getInt(columnName[i]);
                    //数据库类型为int，可能对象类型为boolean，此时需要进行处理
                    if ("boolean".equalsIgnoreCase(paramType.toString())) {
                        if (i1 > 0)
                            md.invoke(o, true);
                        else
                            md.invoke(o, false);
                    } else {
                        md.invoke(o, i1);
                    }
                } else if ("java.lang.String".equals(columnClassName[i])) {
                    md.invoke(o, rs.getString(columnName[i]));
                } else if ("java.lang.Double".equals(columnClassName[i])) {
                    md.invoke(o, rs.getDouble(columnName[i]));
                } else if ("java.sql.Date".equals(columnClassName[i])) {
                    md.invoke(o, rs.getDate(columnName[i]));
                } else if ("java.lang.Boolean".equals(columnClassName[i])) {
                    md.invoke(o, rs.getBoolean(columnName[i]));
                } else if ("java.lang.Float".equals(columnClassName[i])) {
                    md.invoke(o, rs.getFloat(columnName[i]));
                } else if ("java.sql.Time".equals(columnClassName[i])) {
                    md.invoke(o, rs.getTime(columnName[i]));
                } else if ("java.sql.Timestamp".equals(columnClassName[i])) {
                    md.invoke(o, rs.getTimestamp(columnName[i]));
                } else if ("java.lang.Object".equals(columnClassName[i])) {
                    md.invoke(o, rs.getObject(columnName[i]));
                } else if ("java.math.BigDecimal".equals(columnClassName[i])) {
                    md.invoke(o, rs.getBigDecimal(columnName[i]));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (SecurityException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        } catch (InstantiationException e) {
            e.printStackTrace();
            logger.log(Level.SEVERE, "**Error**:" + e.getMessage());
        }
        return o;
    }

    /**
     * @param pstm
     * @param params
     * @throws SQLException
     * @date 2015-4-9
     * 设置SQL语句中的？参数
     */
    private void setParams(PreparedStatement pstm, Object[] params)
            throws SQLException {
        if (params == null | params.length == 0)
            return;
        for (int i = 0; i < params.length; i++) {
            Object param = params[i];
            if (param == null) {
                pstm.setNull(i + 1, Types.NULL);
            } else if (param instanceof Integer) {
                pstm.setInt(i + 1, (Integer) param);
            } else if (param instanceof Double) {
                pstm.setDouble(i + 1, (Double) param);
            } else if (param instanceof Long) {
                pstm.setLong(i + 1, (Long) param);
            } else if (param instanceof String) {
                pstm.setString(i + 1, (String) param);
            } else if (param instanceof Boolean) {
                pstm.setBoolean(i + 1, (Boolean) param);
            } else if (param instanceof java.util.Date) {
                pstm.setTimestamp(i + 1, new java.sql.Timestamp(((java.util.Date) param).getTime()));
            }
        }
    }

}
