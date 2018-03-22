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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Date getRegisterTime() {
        return registerTime;
    }

    public void setRegisterTime(Date registerTime) {
        this.registerTime = registerTime;
    }

    public boolean isUsable() {
        return usable;
    }

    public void setUsable(boolean usable) {
        this.usable = usable;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
