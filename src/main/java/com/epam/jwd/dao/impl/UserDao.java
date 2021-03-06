package com.epam.jwd.dao.impl;

import com.epam.jwd.dao.AbstractDao;
import com.epam.jwd.entity.User;
import com.epam.jwd.pool.ConnectionPool;
import lombok.extern.log4j.Log4j2;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@link UserDao} that sends SQL-requests to database
 * for working with {@link User}.
 *
 * @author Maxim Semenko
 * @version 0.0.1
 */
@Log4j2
public class UserDao implements AbstractDao<User> {

    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final AtomicBoolean INSTANCE_CREATED = new AtomicBoolean(false);
    private static final UserResultSet userResultSet = UserResultSet.getInstance();
    public static ConnectionPool connectionPool = ConnectionPool.getInstance();
    private static UserDao instance;

    private static final String SQL_SELECT_ALL_USERS = "SELECT app_user.*,\n" +
            "       rf.faculty_id,\n" +
            "       uc.average_score,\n" +
            "       uc.russian_score,\n" +
            "       uc.math_score,\n" +
            "       uc.physics_score\n" +
            "FROM app_user\n" +
            "         JOIN registered_faculty rf on app_user.id = rf.user_id\n" +
            "         JOIN user_certificate uc on app_user.id = uc.user_id";

    private static final String SQL_SELECT_USER_BY_ID = "SELECT app_user.*,\n" +
            "       rf.faculty_id,\n" +
            "       uc.average_score,\n" +
            "       uc.russian_score,\n" +
            "       uc.math_score,\n" +
            "       uc.physics_score\n" +
            "FROM app_user\n" +
            "         JOIN registered_faculty rf on app_user.id = rf.user_id\n" +
            "         JOIN user_certificate uc on app_user.id = uc.user_id\n" +
            "WHERE id = ?";

    private static final String SQL_SELECT_MAX_ID = "SELECT MAX(id) FROM app_user";

    private static final String SQL_INSERT_USER =
            "INSERT INTO app_user (id, login, password, firstname, lastname, email, role_id, status_id) VALUES (?,?,?,?,?,?,?,?)";

    private static final String SQL_INSERT_USER_CERTIFICATE
            = "INSERT INTO user_certificate(average_score, russian_score, math_score, physics_score, user_id) VALUES (?,?,?,?,?)";

    private static final String SQL_INSERT_USER_FACULTY =
            "INSERT INTO registered_faculty(user_id, faculty_id) VALUES (?,?)";

    private static final String SQL_INSERT_USER_ENROLLED = "INSERT INTO user_enrolled (user_id, sum_score) VALUES (?,?)";

    private static final String SQL_DELETE_ALL_USERS = "DELETE FROM app_user";

    private static final String SQL_DELETE_ALL_USERS_CERTIFICATE = "DELETE FROM user_certificate";

    private static final String SQL_DELETE_ALL_USERS_FACULTY = "DELETE FROM registered_faculty";

    private static final String SQL_DELETE_USER_BY_ID = "DELETE FROM app_user WHERE id = ?";

    private static final String SQL_DELETE_USER_CERTIFICATE_BY_ID = "DELETE FROM user_certificate WHERE user_id = ?";

    private static final String SQL_DELETE_USER_FACULTY_BY_ID = "DELETE FROM registered_faculty WHERE user_id = ?";

    private static final String SQL_UPDATE_USER = "UPDATE app_user\n" +
            "SET login     = ?,\n" +
            "    password  = ?,\n" +
            "    firstname = ?,\n" +
            "    lastname  = ?,\n" +
            "    email     = ?,\n" +
            "    role_id   = ?,\n" +
            "    status_id = ?\n" +
            "WHERE id = ?";

    private static final String SQL_UPDATE_USER_CERTIFICATE = "UPDATE user_certificate\n" +
            "SET average_score = ?,\n" +
            "    russian_score = ?,\n" +
            "    math_score    = ?,\n" +
            "    physics_score = ?\n" +
            "WHERE user_id = ?";

    private static final String SQL_UPDATE_USER_FACULTY = "UPDATE registered_faculty\n" +
            "SET faculty_id = ?\n" +
            "WHERE user_id = ?";

    private static final String SQL_COUNT_USER_ENROLLED = "SELECT COUNT(*) FROM user_enrolled";

    private static final String SQL_DELETE_ALL_USER_ENROLLED = "DELETE FROM user_enrolled";

    private static final String SQL_SELECT_ID_USER_ENROLLED = "SELECT * FROM user_enrolled";

    public static UserDao getInstance() {
        if (!INSTANCE_CREATED.get()) {
            LOCK.lock();
            try {
                if (instance == null) {
                    instance = new UserDao();
                    INSTANCE_CREATED.set(true);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return instance;
    }

    /**
     * Method that selects all {@link User} from database.
     *
     * @return {@link List<User>}
     */
    @Override
    public List<User> selectAll() {
        List<User> userList = new ArrayList<>();
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_SELECT_ALL_USERS)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                userList.add(userResultSet.execute(resultSet));
            }
            log.info("Select all from app_user");
        } catch (SQLException e) {
            log.info("Error to select all from app_user" + e);
        } finally {
            connectionPool.releaseConnection(connection);
        }
        return userList;
    }

    /**
     * Method that selects {@link User} from database by id.
     *
     * @param id {@link Integer}
     * @return {@link User}
     */
    @Override
    public User selectById(Integer id) {
        User user = null;
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_SELECT_USER_BY_ID)) {
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                user = userResultSet.execute(resultSet);
            }
            log.info("Select user from app_user");
        } catch (SQLException e) {
            log.error("Error to select user from app_user " + e);
        } finally {
            connectionPool.releaseConnection(connection);
            log.info("Connection are returned to pool");
        }
        return user;
    }

    /**
     * Method that inserts {@link User} to database.
     *
     * @param user {@link User}
     * @return {@link Boolean}
     */
    @Override
    public boolean insert(User user) {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement1 = connection.prepareStatement(SQL_INSERT_USER);
             PreparedStatement preparedStatement2 = connection.prepareStatement(SQL_INSERT_USER_CERTIFICATE);
             PreparedStatement preparedStatement3 = connection.prepareStatement(SQL_INSERT_USER_FACULTY);
        ) {
            connection.setAutoCommit(false);
            initPreparedStatement1(preparedStatement1, user);
            preparedStatement1.executeUpdate();
            initPreparedStatement2(preparedStatement2, user);
            preparedStatement2.executeUpdate();
            initPreparedStatement3(preparedStatement3, user);
            preparedStatement3.executeUpdate();
            log.info("User are inserted into database");
            connection.commit();
        } catch (SQLException e) {
            log.error("Can't add user" + e);
            try {
                connection.rollback();
                log.error("Connection rollback");
            } catch (SQLException ex) {
                log.error("Can't rollback connection " + ex);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
                connectionPool.releaseConnection(connection);
                log.info("Connection are returned to pool");
            } catch (SQLException e) {
                log.error("Can't release a connection " + e);
            }
        }
        return true;
    }

    /**
     * Method that update {@link User}.
     *
     * @param user {@link User}
     */
    @Override
    public void update(User user) {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement1 = connection.prepareStatement(SQL_UPDATE_USER);
             PreparedStatement preparedStatement2 = connection.prepareStatement(SQL_UPDATE_USER_CERTIFICATE);
             PreparedStatement preparedStatement3 = connection.prepareStatement(SQL_UPDATE_USER_FACULTY);
        ) {
            connection.setAutoCommit(false);
            initPreparedStatement1(preparedStatement1, user);
            preparedStatement1.executeUpdate();
            initPreparedStatement2(preparedStatement2, user);
            preparedStatement2.executeUpdate();
            initPreparedStatement3(preparedStatement3, user);
            preparedStatement3.executeUpdate();
            log.info("User are updated into database");
            connection.commit();
        } catch (SQLException e) {
            log.error("Can't update user" + e);
            try {
                connection.rollback();
                log.error("Connection rollback");
            } catch (SQLException ex) {
                log.error("Can't rollback connection " + ex);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
                connectionPool.releaseConnection(connection);
                log.info("Connection are returned to pool");
            } catch (SQLException e) {
                log.error("Can't release a connection " + e);
            }
        }
    }

    /**
     * Method that removes {@link User} from database by id.
     *
     * @param id {@link Integer}
     * @return {@link Boolean}
     */
    @Override
    public boolean removeById(Integer id) {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement1 = connection.prepareStatement(SQL_DELETE_USER_CERTIFICATE_BY_ID);
             PreparedStatement preparedStatement2 = connection.prepareStatement(SQL_DELETE_USER_FACULTY_BY_ID);
             PreparedStatement preparedStatement3 = connection.prepareStatement(SQL_DELETE_USER_BY_ID);
        ) {
            connection.setAutoCommit(false);
            preparedStatement1.setInt(1, id);
            preparedStatement1.executeUpdate();
            preparedStatement2.setInt(1, id);
            preparedStatement2.executeUpdate();
            preparedStatement3.setInt(1, id);
            preparedStatement3.executeUpdate();
            log.info("User are removed from app_user");
            connection.commit();
        } catch (SQLException e) {
            log.error("Can't remove user" + e);
            try {
                connection.rollback();
                log.info("Connection rollback");
            } catch (SQLException ex) {
                log.error("Can't rollback connection " + ex);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
                connectionPool.releaseConnection(connection);
                log.info("Connection are returned to pool");
            } catch (SQLException e) {
                log.error("Can't release a connection " + e);
            }
        }
        return true;
    }

    /**
     * Method that removes all {@link User} from database.
     */
    public void removeAllUsers() {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement1 = connection.prepareStatement(SQL_DELETE_ALL_USERS_CERTIFICATE);
             PreparedStatement preparedStatement2 = connection.prepareStatement(SQL_DELETE_ALL_USERS_FACULTY);
             PreparedStatement preparedStatement3 = connection.prepareStatement(SQL_DELETE_ALL_USERS);
        ) {
            connection.setAutoCommit(false);
            preparedStatement1.executeUpdate();
            preparedStatement2.executeUpdate();
            preparedStatement3.executeUpdate();
            log.info("User are removed from app_user");
            connection.commit();
        } catch (SQLException e) {
            log.error("Can't remove user" + e);
            try {
                connection.rollback();
                log.info("Connection rollback");
            } catch (SQLException ex) {
                log.error("Can't rollback connection " + ex);
            }
        } finally {
            try {
                connection.setAutoCommit(true);
                connectionPool.releaseConnection(connection);
                log.info("Connection are returned to pool");
            } catch (SQLException e) {
                log.error("Can't release a connection " + e);
            }
        }
    }

    /**
     * Method that returns max id from database.
     *
     * @return {@link Integer}
     */
    @Override
    public int getMaxId() {
        Connection connection = getConnection();
        int id = -1;
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_SELECT_MAX_ID)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                id = resultSet.getInt(1);
            }
            log.info("Max id are selected from app_user");
        } catch (SQLException e) {
            log.error("Error to select max id from app_user " + e);
        } finally {
            connectionPool.releaseConnection(connection);
            log.info("Connection are returned to pool");
        }
        return id;
    }

    /**
     * Method that inserts {@link User} to enrolled list.
     *
     * @param user {@link User}
     */
    public void insertToEnrolledList(User user) {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_USER_ENROLLED)) {
            preparedStatement.setInt(1, user.getId());
            preparedStatement.setInt(2, user.getSumExams());
            preparedStatement.executeUpdate();
            log.info("User are inserted into user_enrolled");
        } catch (SQLException e) {
            log.error("Error to add user into user_enrolled " + e);
        } finally {
            connectionPool.releaseConnection(connection);
            log.info("Connection are returned to pool");
        }
    }

    /**
     * Method that removes all {@link User} from enrolled list.
     */
    public void removeAllFromEnrolledList() {
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_DELETE_ALL_USER_ENROLLED)) {
            preparedStatement.executeUpdate();
            log.info("All users are removed from user_enrolled");
        } catch (SQLException e) {
            log.error("Error to remove all users from user_enrolled " + e);
        } finally {
            connectionPool.releaseConnection(connection);
            log.info("Connection are returned to pool");
        }
    }

    /**
     * Method that returns count of user enrolled.
     *
     * @return {@link Integer}
     */
    public int getCountUserEnrolled() {
        Connection connection = getConnection();
        int count = -1;
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_COUNT_USER_ENROLLED)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                count = resultSet.getInt(1);
            }
            log.info("Count from user_enrolled");
        } catch (SQLException e) {
            log.error("Error to count from user_enrolled " + e);
        } finally {
            connectionPool.releaseConnection(connection);
            log.info("Connection are returned to pool");
        }
        return count;
    }

    /**
     * Method that return {@link Map<>} with enrolled {@link User}.
     *
     * @return {@link Map<>}
     */
    public Map<Integer, Integer> selectAllEnrolledList() {
        Map<Integer, Integer> integerMap = new HashMap<>();
        Connection connection = getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(SQL_SELECT_ID_USER_ENROLLED)) {
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                integerMap.put(resultSet.getInt(1), resultSet.getInt(2));
            }
            log.info("Select all id from user_enrolled");
        } catch (SQLException e) {
            log.info("Error to select all id from user_enrolled" + e);
        } finally {
            connectionPool.releaseConnection(connection);
            log.info("Connection are returned to pool");
        }
        return integerMap;
    }

    /**
     * Method that inits {@link PreparedStatement}.
     *
     * @param preparedStatement1 {@link PreparedStatement}
     * @param user               {@link User}
     * @throws SQLException exception
     */
    private void initPreparedStatement1(PreparedStatement preparedStatement1, User user) throws SQLException {
        preparedStatement1.setInt(1, user.getId());
        preparedStatement1.setString(2, user.getLogin());
        preparedStatement1.setString(3, user.getPassword());
        preparedStatement1.setString(4, user.getFirstname());
        preparedStatement1.setString(5, user.getLastname());
        preparedStatement1.setString(6, user.getEmail());
        preparedStatement1.setInt(7, user.getUserRole().getId());
        preparedStatement1.setInt(8, user.getUserStatus().getId());
    }

    /**
     * Method that inits {@link PreparedStatement}.
     *
     * @param preparedStatement2 {@link PreparedStatement}
     * @param user               {@link User}
     * @throws SQLException exception
     */
    private void initPreparedStatement2(PreparedStatement preparedStatement2, User user) throws SQLException {
        preparedStatement2.setInt(1, user.getAverageScore());
        preparedStatement2.setInt(2, user.getRussianExamScore());
        preparedStatement2.setInt(3, user.getMathExamScore());
        preparedStatement2.setInt(4, user.getPhysicsExamScore());
        preparedStatement2.setInt(5, user.getId());
    }

    /**
     * Method that inits {@link PreparedStatement}.
     *
     * @param preparedStatement3 {@link PreparedStatement}
     * @param user               {@link User}
     * @throws SQLException exception
     */
    private void initPreparedStatement3(PreparedStatement preparedStatement3, User user) throws SQLException {
        preparedStatement3.setInt(1, user.getId());
        preparedStatement3.setInt(2, user.getFacultyId());
    }

}
