package ru.job4j.grabber;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.job4j.connect.ConnectSQL;
import ru.job4j.connect.IProperties;

public class PsqlStore implements Store, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectSQL.class.getName());
    private Connection cnn;

    public PsqlStore(IProperties properties) {
        cnn = new ConnectSQL().get(properties);
    }

    @Override
    public void save(IPost post) {
        try (PreparedStatement statement =
                     cnn.prepareStatement(
                             "insert into posts (name, text, link, date) values (?, ?, ?, ?)")) {
            statement.setString(1, post.getName());
            statement.setString(2, post.getText());
            statement.setString(3, post.getLink());
            statement.setTimestamp(4, Timestamp.valueOf(post.getLocalDateTime()));
            if (statement.execute()) {
                LOG.warn("Данные поста не добавились в таблицу SQL: {}", post);
            } else {
                LOG.debug("Запись поста добавлена: {}", post);
            }
        } catch (Exception e) {
            LOG.error("Ошибка добавления в таблицу SQL: {}", post, e);
        }
    }

    @Override
    public List<IPost> getAll() {
        List<IPost> posts = new LinkedList<>();
        try (PreparedStatement statement =
                     cnn.prepareStatement(
                             "SELECT name, text, link, date FROM posts")) {
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    posts.add(new Post(
                            resultSet.getString("name"),
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getTimestamp("date").toLocalDateTime()));
                }
            }
        } catch (Exception e) {
            LOG.error("Ошибка получения данных SQL: ", e);
        }
        return posts;
    }

    @Override
    public IPost findById(Integer postId) {
        IPost post = null;
        try (PreparedStatement statement =
                     cnn.prepareStatement(
                             "SELECT name, text, link, date FROM posts WHERE post_id = ?")) {
            statement.setInt(1, postId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    LOG.warn("Не найдена запись поста с post_id = {}", postId);
                } else {
                    post = new Post(
                            resultSet.getString("name"),
                            resultSet.getString("text"),
                            resultSet.getString("link"),
                            resultSet.getTimestamp("date").toLocalDateTime());
                }
            }
        } catch (Exception e) {
            LOG.error("Ошибка получения данных SQL: ", e);
        }
        return post;
    }

    @Override
    public void close() throws Exception {
        if (cnn != null) {
            cnn.close();
        }
    }

    public static void main(String[] args) {
        PsqlStore psqlStore = new PsqlStore(new PsqlProperties());
        IPost post = new Post("test", "test", "http://test.com", LocalDateTime.now());
        psqlStore.save(post);
        psqlStore.getAll().forEach(System.out::println);
        System.out.println("----");
        System.out.println(psqlStore.findById(1));
    }

    private static class Post implements IPost {
        private String name;
        private String text;
        private String link;
        private LocalDateTime localDateTime;

        public Post(String name, String text, String link, LocalDateTime localDateTime) {
            this.name = name;
            this.text = text;
            this.link = link;
            this.localDateTime = localDateTime;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getText() {
            return text;
        }

        @Override
        public String getLink() {
            return link;
        }

        @Override
        public LocalDateTime getLocalDateTime() {
            return localDateTime;
        }

        @Override
        public String toString() {
            return "Post{"
                    + "name='" + name + '\''
                    + ", text='" + text + '\''
                    + ", link='" + link + '\''
                    + ", localDateTime=" + localDateTime
                    + '}';
        }
    }
}

