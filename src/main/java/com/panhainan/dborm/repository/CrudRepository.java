package com.panhainan.dborm.repository;

import com.panhainan.dborm.annotation.Column;
import com.panhainan.dborm.annotation.Id;
import com.panhainan.dborm.annotation.Table;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class CrudRepository<T> extends Repository<T> {

    private Logger logger = Logger.getLogger(this.toString());

    public T get(int id, Class<T> t) {
        T obj = null;
        StringBuilder sql = new StringBuilder();
        try {
            obj = t.newInstance();
            Class c = obj.getClass();
            if (!c.isAnnotationPresent(Table.class)) {
                return null;
            }
            sql.append("select * from ")
                    .append(((Table) c.getAnnotation(Table.class)).value());
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    sql.append(" where ")
                            .append(field.getAnnotation(Id.class).value())
                            .append("=")
                            .append(id);
                    break;
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        logger.info(sql.toString());
        obj = super.executeGet(sql.toString(), t);
        return obj;
    }

    /**
     * @param t
     * @return 返回t在数据库的id
     */
    public int save(T t) {
        Class c = t.getClass();
        if (!c.isAnnotationPresent(Table.class)) {
            return 0;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("insert into ")
                .append(((Table) c.getAnnotation(Table.class)).value())
                .append("(");
        Field[] fields = c.getDeclaredFields();
        List<String> fieldNames = new ArrayList<String>();
        List<String> columnNames = new ArrayList<String>();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                columnNames.add(field.getAnnotation(Column.class).value());
                fieldNames.add(field.getName());
            }
        }
        StringBuilder paramValue = new StringBuilder(" values(");
        int i = 0;
        for (; i < fieldNames.size(); i++) {
            sql.append(columnNames.get(i)).append(",");
            Object fieldValue = null;
            try {
                StringBuilder sb = new StringBuilder(fieldNames.get(i));
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                String methodName;
                String fieldNameType = c.getDeclaredField(fieldNames.get(i)).getGenericType().toString();
                if (fieldNameType.equalsIgnoreCase("boolean")) {
                    methodName = "is" + sb.toString();
                } else {
                    methodName = "get" + sb.toString();
                }
                Method method = c.getMethod(methodName);
                fieldValue = method.invoke(t);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
            }
            paramValue.append(generateStringByFieldValueType(fieldValue));
            paramValue.append(",");
        }
        sql.setCharAt(sql.length() - 1, ')');
        paramValue.setCharAt(paramValue.length() - 1, ')');
        sql.append(paramValue);
        logger.info(sql.toString());
        return super.executeInsert(sql.toString());
    }

    public int update(T t) {
        Class c = t.getClass();
        if (!c.isAnnotationPresent(Table.class)) {
            return 0;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("update ")
                .append(((Table) c.getAnnotation(Table.class)).value())
                .append(" set ");
        Field[] fields = c.getDeclaredFields();
        StringBuilder whereStr = new StringBuilder("where ");
        for (Field field : fields) {
            if (field.isAnnotationPresent(Column.class)) {
                StringBuilder sb = new StringBuilder(field.getName());
                sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                String methodName;
                String fieldNameType = field.getGenericType().toString();
                if (fieldNameType.equalsIgnoreCase("boolean")) {
                    methodName = "is" + sb.toString();
                } else {
                    methodName = "get" + sb.toString();
                }
                Method method = null;
                Object fieldValue = null;
                try {
                    method = c.getMethod(methodName);
                    fieldValue = method.invoke(t);
                    if (fieldValue == null) {
                        continue;
                    }
                    sql.append(field.getAnnotation(Column.class).value()).append("=");
                    sql.append(generateStringByFieldValueType(fieldValue)).append(",");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } else if (field.isAnnotationPresent(Id.class)) {
                whereStr.append(field.getAnnotation(Id.class).value()).append("=");
                Method method;
                Object fieldValue = null;
                try {
                    StringBuilder sb = new StringBuilder(field.getName());
                    sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
                    String methodName;
                    String fieldNameType = field.getGenericType().toString();
                    if (fieldNameType.equalsIgnoreCase("boolean")) {
                        methodName = "is" + sb.toString();
                    } else {
                        methodName = "get" + sb.toString();
                    }
                    method = c.getMethod(methodName);
                    fieldValue = method.invoke(t);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                whereStr.append(generateStringByFieldValueType(fieldValue));
            }
        }
        sql.setCharAt(sql.length() - 1, ' ');
        sql.append(whereStr);
        logger.info(sql.toString());
        return super.executeUpdateAndDelete(sql.toString());
    }

    public int remove(int id, Class<T> t) {
        T obj = null;
        StringBuilder sql = new StringBuilder();
        try {
            obj = t.newInstance();
            Class c = obj.getClass();
            if (!c.isAnnotationPresent(Table.class)) {
                return 0;
            }
            sql.append("delete from ")
                    .append(((Table) c.getAnnotation(Table.class)).value());
            Field[] fields = c.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Id.class)) {
                    sql.append(" where ")
                            .append(field.getAnnotation(Id.class).value())
                            .append("=")
                            .append(id);
                    break;
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        logger.info(sql.toString());
        return super.executeUpdateAndDelete(sql.toString());
    }

    public List<T> list(int startRecord, int pageSize, Class<T> t) {
        T obj = null;
        StringBuilder sql = new StringBuilder();
        try {
            obj = t.newInstance();
            Class c = obj.getClass();
            if (!c.isAnnotationPresent(Table.class)) {
                return null;
            }
            Table table = (Table) c.getAnnotation(Table.class);
            String tableName = table.value();
            sql.append("select * from ")
                    .append(tableName)
                    .append(" limit ")
                    .append(startRecord)
                    .append(",").append(pageSize);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        logger.info(sql.toString());
        return super.executeList(sql.toString(), t);
    }

    public int count(Class<T> t) {
        T obj = null;
        StringBuilder sql = new StringBuilder();
        try {
            obj = t.newInstance();
            Class c = obj.getClass();
            if (!c.isAnnotationPresent(Table.class)) {
                return 0;
            }
            Table table = (Table) c.getAnnotation(Table.class);
            String tableName = table.value();
            sql.append("select count(*) from ")
                    .append(tableName);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        logger.info(sql.toString());
        return super.getCountRow(sql.toString());

    }

    /**
     * 判断属性值得类型来设置进行sql拼接时是否需要加“”或其他处理
     *
     * @param fieldValue
     * @return
     */
    private String generateStringByFieldValueType(Object fieldValue) {
        StringBuilder stringBuilder = new StringBuilder();
        if (fieldValue instanceof String) {
            stringBuilder.append("\"").append(fieldValue).append("\"");
        } else if (fieldValue instanceof java.util.Date) {
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss");
            fieldValue = simpleDateFormat.format(fieldValue);
            stringBuilder.append("\"").append(fieldValue).append("\"");
        } else {
            stringBuilder.append(fieldValue);
        }
        return stringBuilder.toString();
    }


}
