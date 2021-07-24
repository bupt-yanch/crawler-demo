package org.dssc.demo.storage;

import com.mysql.cj.util.StringUtils;
import org.dssc.demo.data.Article;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ArticleStorage {

    private static final String TABLE = "document_info";
    @Autowired
    private JdbcTemplate jdbcTemplate;

    public List<Article> getAllArticle() {
        String sql = "SELECT * FROM document_info";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public void insert(Article article) throws DuplicateKeyException {
        String sql = "INSERT INTO " + TABLE +
                " (website, url, `date`, title, abstractText, kind, createdTime)" +
                " VALUES(?,?,?,?,?,?,?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql,
                article.getWebsite(),
                article.getUrl(),
                article.getDate(),
                article.getTitle(),
                article.getAbstractText(),
                StringUtils.isNullOrEmpty(article.getKind()) ? "" : article.getKind(),
                System.currentTimeMillis());
    }

    private static final RowMapper<Article> ROW_MAPPER = (rs, rowNum) -> {
        Article article = new Article();
        article.setId(rs.getInt("id"));
        article.setWebsite(rs.getString("website"));
        article.setUrl(rs.getString("url"));
        article.setDate(rs.getString("date"));
        article.setTitle(rs.getString("title"));
        article.setAbstractText(rs.getString("abstractText"));
        article.setKind(rs.getString("kind"));
        article.setCreatedTime(rs.getLong("createdTime"));
        return article;
    };
}
