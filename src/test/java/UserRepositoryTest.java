import org.junit.Test;

import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class UserRepositoryTest {

    private Logger logger = Logger.getLogger(this.toString());

    @Test
    public void get(){
        UserRepository userRepository = new UserRepository();
        User user = userRepository.get(1,User.class);
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
        List<User> users = userRepository.list(1,2,User.class);
        logger.info(users.toString());

    }
    @Test
    public void count(){
        UserRepository userRepository = new UserRepository();
        int result = userRepository.count(User.class);
        logger.info("result:"+result);
    }
}
