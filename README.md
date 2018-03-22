
　　基于Java自定义注解、反射和JDBC的简单对象ORM映射框架，包含简单的CRUD和Page。这一篇文章基本上都是源码，静下心来看肯定会有收获的，有疑问或者有建议都可以通过邮箱或者QQ等方式连接作者，作者很乐意和你一起探讨。对于本文的代码，可以直接前往GitHub上获取，地址见文章末尾处。

　　首先定义三个注解@Table、@Id、@Column，分别用于表示数据库表、数据库表中的ID字段、数据库表中的其他字段。代码分别如下：

```java
import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Table {
    String value();
}
```
```java
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Id {
    String value();
}
```
```java
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Column {
    String value();
}
```

　　这里用到几年前写的一个DBUtil数据库连接工具类：
```java

import java.sql.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author panhainan
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
                    logger.log(Level.INFO, "Database rs closed .");
                }
            } finally {
                try {
                    if (pstm != null) { // 当Statement对象的实例stmt不为空时
                        pstm.close(); // 关闭Statement对象
                        logger.log(Level.INFO, "Database pstm closed .");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (conn != null) { // 当Connection对象的实例conn不为空时
                        conn.close(); // 关闭Connection对象
                        logger.log(Level.INFO, "Database connect closed .");
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
```
　　以及一个对数据库进行CRUD和Page的泛型工具类：
```java
import com.panhainan.dborm.uitl.DBUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Repository<T> {

    private Logger logger = Logger.getLogger(this.toString());

    /**
     * @param insertSql
     * @return 插入的数据的id
     * @date 2015-4-9
     * @TODO 执行插入语句
     */
    protected int executeInsert(String insertSql) {
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
     * @date 2015-4-9
     * @TODO 执行更新语句或者删除语句
     */
    protected int executeUpdateAndDelete(String updateSql) {
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
     * @date 2015-4-10
     * @TODO 通过数据库标识字段id查找
     */
    protected T executeGet(String sql, Class<T> obj) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
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
     * @date 2015-4-10
     * @TODO 根据参数params获取list
     */
    protected List<T> executeList(String sql, Class<T> c) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
        List<T> listObjects = null;
        T o = null;
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
     * @param params
     * @return
     * @date 2015-4-11
     * @TODO 获取总数
     */
    protected int getCountRow(String sql, Object... params) {
        Connection conn = DBUtil.getConnection();
        PreparedStatement pstm = null;
        ResultSet rs = null;
        int countRow = 0;
        try {
            pstm = conn.prepareStatement(sql);
            setParams(pstm, params);
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
     * @TODO 将数据库中查询出来的结果集ResultSet转化为实体
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
     * @TODO 设置SQL语句中的？参数
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

```
　　然后我们在这个泛型工具类的基础上实现一个CRUD和Page的对自定义注解解析的工具类：
```java
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

```
　　到此，基本完成，配置好db.properties，如下：
```properties
#####################mysql#######################
driverClassName=com.mysql.jdbc.Driver
url=jdbc:mysql://localhost:3306/dborm?useUnicode=true&characterEncoding=utf-8
username=root
password=123456
```
　　接下来就是测试工作。
　　新建一个User类，如下：
```java
import com.panhainan.dborm.annotation.Column;
import com.panhainan.dborm.annotation.Id;
import com.panhainan.dborm.annotation.Table;

import java.util.Date;

@Table("t_user")
public class User {
    @Id("id")
    private long id;
    @Column("account")
    private String account;
    @Column("password")
    private String password;
    @Column("real_name")
    private String realName;
    @Column("age")
    private int age;
    @Column("register_time")
    private Date registerTime;
    @Column("usable")
    private boolean usable;
    public User() {
    }

    public User(long id,String realName, int age, Date registerTime, boolean usable) {
        this.id = id;
        this.realName = realName;
        this.age = age;
        this.registerTime = registerTime;
        this.usable = usable;
    }

    public User(String account, String password, String realName, int age, Date registerTime, boolean usable) {
        this.account = account;
        this.password = password;
        this.realName = realName;
        this.age = age;
        this.registerTime = registerTime;
        this.usable = usable;
    }

    public User(long id, String account, String password, String realName, int age, Date registerTime, boolean usable) {
        this.id = id;
        this.account = account;
        this.password = password;
        this.realName = realName;
        this.age = age;
        this.registerTime = registerTime;
        this.usable = usable;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", account='" + account + '\'' +
                ", password='" + password + '\'' +
                ", realName='" + realName + '\'' +
                ", age=" + age +
                ", registerTime=" + registerTime +
                ", usable=" + usable +
                '}';
    }

    此处为对应get,set方法....
}

```
　　新建一个UserRepository类用来实现User对象的持久化操作，需要继承CrudRepository类，如下：

　　最后新建一个测试类UserRepositoryTest类，如下：
```java
import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class UserRepositoryTest {

    private Logger logger = Logger.getLogger(this.toString());

    @Test
    public void get(){
        UserRepository userRepository = new UserRepository();
        User user = userRepository.get(3,User.class);
        logger.info(user.toString());

    }
    @Test
    public void save(){
        UserRepository userRepository = new UserRepository();
        User user = new User("admin1","1111","管理员1",10,new Date(),false);
        int result = userRepository.save(user);
        logger.info(String.valueOf(result));
    }
    @Test
    public void update(){
        UserRepository userRepository = new UserRepository();
        User user = new User(1,"张三",50,new Date(),false);
        int result = userRepository.update(user);
        logger.info(String.valueOf(result));
    }
    @Test
    public void remove(){
        UserRepository userRepository = new UserRepository();
        int result  = userRepository.remove(3,User.class);
        logger.info("result:"+result);
    }

    @Test
    public void list(){
        UserRepository userRepository = new UserRepository();
        List<User> users = userRepository.list(0,2,User.class);
        logger.info(users.toString());
    }
    @Test
    public void count(){
        UserRepository userRepository = new UserRepository();
        int result = userRepository.count(User.class);
        logger.info("result:"+result);
    }
}
```
　　运行即可查看到对应结果。

　　源码请前往GitHub查看，地址：<a href="https://github.com/panhainan/dborm" target="_blank">github.com/panhainan/dborm</a>