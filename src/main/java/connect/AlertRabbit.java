package connect;

import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;
import static org.quartz.SimpleScheduleBuilder.*;

/**
 * Чтобы этого избежать коннект к базу будет создаваться при старте. Объект коннект будет передавать в Job.
 *
 * Quartz создает объект Job, каждый раз при выполнении работы.
 * Каждый запуск работы вызывает конструтор. Чтобы в объект Job иметь общий ресурс нужно использовать JobExecutionContext.
 *
 * При создании Job мы указывает параметры data. В них мы передаем ссылку на store.
 * В нашем примере store это ArrayList.
 *
 * Чтобы получить объекты из context используется следующий вызов.
 * List<Long> store = (List<Long>) context.getJobDetail().getJobDataMap().get("store");
 * Объект store является общим для каждой работы.
 */
public class AlertRabbit implements AutoCloseable {
    private static Connection connection;

    public static void main(String[] args) {
        try {
            List<Long> store = new ArrayList<>();
            Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
            JobDataMap data = new JobDataMap();
            data.put("recordTimeNow", store);
            JobDetail job = newJob(Rabbit.class)
                    .usingJobData(data)
                    .build();
            SimpleScheduleBuilder times = simpleSchedule()
                    .withIntervalInSeconds(5)
                    .repeatForever();
            Trigger trigger = newTrigger()
                    .startNow()
                    .withSchedule(times)
                    .build();
            scheduler.scheduleJob(job, trigger);
            Thread.sleep(10000);
            scheduler.shutdown();
            System.out.println("store " + store);
            save(store);

        } catch (Exception se) {
            se.printStackTrace();
        }
    }

    /**
     * делаем конект
     *
     * @return коннект какой в апп написан
     */
    private Connection init() {
        try (InputStream in = AlertRabbit.class.getClassLoader().getResourceAsStream("app.properties")) {
            Properties config = new Properties();
            config.load(in);
            Class.forName(config.getProperty("driver-class-name"));
            connection = DriverManager.getConnection(
                    config.getProperty("url"),
                    config.getProperty("username"),
                    config.getProperty("password")
            );
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return connection;
    }

    /**
     * делаем запись листа
     * вставками каждый раз
     * есть метод банч вроде он пачкой делает хапись уточнить !!!
     *
     * @param time лист с времнем с джоба
     */
    private static void save(List<Long> time) throws SQLException {
        connection = new AlertRabbit().init();
        for (Long timer : time) {
            Date d = new Date(timer * 1000);
            System.out.println(d);
            try (PreparedStatement ps = connection.prepareStatement("insert into rabbit (create_date) values (?)")) {
                ps.setDate(1, d);
                ps.execute();
            }
        }
    }

    /**
     * не помню зачем тту ведь в тру ресурс но все пишут закрывать вот и мы закрываем
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * класс который что то делает
     */
    public static class Rabbit implements Job {

        public Rabbit() {
            System.out.println(hashCode());
        }

        /**
         * главный метод который наверно в JobRunShell включаеться в методе run
         * @param context
         * @throws JobExecutionException
         */
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            System.out.println("Rabbit runs here ...");
            // !!!!!!!!!!! не совсем понятен момент
            List<Long> store = (List<Long>) context.getJobDetail().getJobDataMap().get("recordTimeNow");
            store.add(System.currentTimeMillis());
        }
    }
}