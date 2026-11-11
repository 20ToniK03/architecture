package controller.favorite;

import java.util.Date;

/**
 * Бизнес-объект «Избранное» (UML: {@code Favorite} - {@code id}, {@code addedAt}).
 * Связывает пользователя с сохранённой новостью (use case «Избранное»).
 */
public class FavoriteBO {

    private Long id;
    private Date addedAt;
    private Long newsId;

    public FavoriteBO() {
    }

    public FavoriteBO(Long id, Date addedAt, Long newsId) {
        this.id = id;
        this.addedAt = addedAt;
        this.newsId = newsId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Date addedAt) {
        this.addedAt = addedAt;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }
}
