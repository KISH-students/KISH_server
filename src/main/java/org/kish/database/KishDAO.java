package org.kish.database;

import org.kish.KishServer;
import org.kish.database.mapper.ExamMapper;
import org.kish.database.mapper.LunchMenuMapper;
import org.kish.entity.Exam;
import org.kish.entity.LunchMenu;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.text.SimpleDateFormat;
import java.util.*;


@Repository
public class KishDAO {
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    public JdbcTemplate jdbcTemplate;

    public KishDAO(DataSource dataSource){
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        KishServer.jdbcTemplate = this.jdbcTemplate;
    }

    public int addAdmin(String seq){
        String query
                = "INSERT INTO `kish_admin` (`seq`) VALUES (?);";
        return jdbcTemplate.update(query, seq);
    }

    public int removeAdmin(String seq){
        String query = "DELETE FROM `kish_admin` WHERE `seq` = ?";
        return jdbcTemplate.update(query, seq);
    }

    public boolean isAdmin(String seq){
        String query = "SELECT * FROM `kish_admin` WHERE `seq`=?";
        return jdbcTemplate.queryForList(query, seq).size() > 0;
    }

    public List<String> getDeviceIdByTopic(String topic){
        String query = "SELECT * FROM `kish_notification` " +
                "WHERE `topic` = '" + topic + "'";

        ArrayList<String> list = new ArrayList<>();
        for (Map<String, Object> user : jdbcTemplate.queryForList(query)) {
            list.add((String) user.get("device_id"));
        }

        return list;
    }

    public int addUserToTopic(String topic, String deviceID){
        if(!KishServer.firebaseManager.isExistUser(deviceID)) return -1;
        if(isUserInTopic(topic, deviceID)) return -2;

        String query
                = "INSERT INTO `kish_notification` (`topic`, `device_id`) " +
                "VALUES " +
                "  (?, ?);";

        return jdbcTemplate.update(query, topic, deviceID);
    }

    public int removeUserFromTopic(String topic, String deviceID){
        String query = "DELETE FROM `kish_notification` " +
                "WHERE `device_id` = ? AND `topic` = ?";
        return jdbcTemplate.update(query, deviceID, topic);
    }

    public int removeUserFromAllTopics(String deviceID){
        String query = "DELETE FROM `kish_notification` " +
                "WHERE `device_id` = ?";
        return jdbcTemplate.update(query, deviceID);
    }

    public int removeUsersFromAllTopics(ArrayList<String> deviceIDs){
        StringBuilder sb = new StringBuilder();
        String query = "DELETE FROM `kish_notification` " +
                "WHERE `device_id` in (?)";

        for (String deviceID : deviceIDs) {
            sb.append(",'").append(deviceID).append("'");
        }

        return jdbcTemplate.update(query, sb.toString());
    }

    public boolean isUserInTopic(String topic, String deviceID){
        String query = "SELECT COUNT(*) FROM `kish_notification` " +
                "WHERE `topic` = '" + topic + "' " +
                "AND `device_id` = '" + deviceID + "'";
        return jdbcTemplate.queryForObject(query, Integer.class) > 0;
    }

    public int addPlanToCalendar(Calendar date, String plan){
        String strDate = sdf.format(date.getTime());
        String query
                = "INSERT INTO `kish_calendar`(`date`, `plan`)\n" +
                "VALUES(?, ?);";

        return jdbcTemplate.update(query, strDate, plan);
    }

    public int removePlanFromCalendar(Calendar date, String plan){
        String strDate = sdf.format(date.getTime());
        String query
                = "DELETE FROM `kish_calendar` WHERE `date` = ? AND `plan` = ?";

        return jdbcTemplate.update(query, strDate, plan);
    }

    // Year and month
    public List<Map<String, Object>> getPlansByYM(Calendar date){
        date.set(Calendar.DAY_OF_MONTH, 1);
        String startDate = sdf.format(date.getTime());
        date.set(Calendar.DAY_OF_MONTH, date.getActualMaximum(Calendar.DAY_OF_MONTH));
        String endDate = sdf.format(date.getTime());

        String query = "SELECT * FROM `kish_calendar` " +
                "WHERE `date` " +
                "BETWEEN '" + startDate + "' " +
                "AND '" + endDate + "'";
        return jdbcTemplate.queryForList(query);
    }

    public List<Exam> getExamDates(){
        String query = "SELECT * FROM `kish_exam`";
        return jdbcTemplate.query(query, new ExamMapper());
    }

    public List<LunchMenu> queryLunchMenu(Calendar date){
        date.set(Calendar.DAY_OF_MONTH, 1);
        String startDate = sdf.format(date.getTime());
        date.set(Calendar.DAY_OF_MONTH, date.getActualMaximum(Calendar.DAY_OF_MONTH));
        String endDate = sdf.format(date.getTime());

        String query = "SELECT * FROM `kish_lunch` " +
                "WHERE `lunch_date` " +
                "BETWEEN '" + startDate + "' " +
                "AND '" + endDate + "'";
        return jdbcTemplate.query(query, new LunchMenuMapper());
    }

    public void updateLunchMenus(boolean async, ArrayList<LunchMenu> list){
        if(async) {
            updateLunchMenus(false, list);
            return;
        }

        String query = "INSERT INTO `kish_lunch` (lunch_date, menu, dinner_menu, detail, image_url) " +
                "VALUES (?, ?, \"정보 없음\", ?, ?) " +
                "ON DUPLICATE KEY UPDATE menu=if(CHAR_LENGTH(`menu`) < 6, ?, `menu`), detail=?, image_url=?; ";
        ArrayList<Object[]> params = new ArrayList<>();

        for (LunchMenu lunchMenu : list) {
            Object[] args = new Object[]{
                    lunchMenu.getDate(),lunchMenu.getMenu(),lunchMenu.getDetail(),lunchMenu.getImageUrl(),
                    lunchMenu.getMenu(), lunchMenu.getDetail(), lunchMenu.getImageUrl()};
            params.add(args);
        }

        jdbcTemplate.batchUpdate(query, params);
    }

    public void registerUser(String seq, String fcm) {
        String sql = "INSERT INTO `kish_users` SELECT NULL,?,? FROM DUAL" +
                " WHERE NOT EXISTS (SELECT * FROM `kish_users` WHERE `user_id` = ? AND `fcm_token` = ?);";

        jdbcTemplate.update(sql, seq, fcm, seq, fcm);
    }

    public boolean isValidUser(String seq, String fcm) {
        List<String> fcmList = this.getUserFcm(seq);

        if (fcmList.contains(fcm)) {    // 유저의 계정에 등록된 fcm일 경우
            return true;
        } else {
            return false;
        }
    }

    public List<String> getUserFcm(String seq) {
        String sql = "SELECT * FROM `kish_users` WHERE `user_id`=?";
        List<Map<String, Object>> items = jdbcTemplate.queryForList(sql, seq);

        ArrayList<String> fcmList = new ArrayList<>();
        for (Map<String, Object> item : items) {
            fcmList.add((String) item.get("fcm_token"));
        }

        return fcmList;
    }
}
