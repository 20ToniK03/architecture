package controller.comment;

import controller.person.UserBO;

import java.util.Date;

/**
 * Бизнес-объект «Комментарий» (UML: {@code Comment}).
 *
 * <p>Хранит ссылку на новость ({@code newsId}) и автора ({@code User 1 -- 0..* Comment}).</p>
 */
public class CommentBO {

    private Long id;
    private Long newsId;
    private String text;
    private Date createdAt;
    private UserBO author;
    private boolean approved;

    public CommentBO() {
    }

    public CommentBO(Long id, Long newsId, String text, Date createdAt, UserBO author) {
        this.id = id;
        this.newsId = newsId;
        this.text = text;
        this.createdAt = createdAt;
        this.author = author;
        this.approved = true;
    }

    /** Редактировать текст комментария (метод {@code edit} из UML). */
    public void edit(String text) {
        this.text = text;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNewsId() {
        return newsId;
    }

    public void setNewsId(Long newsId) {
        this.newsId = newsId;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public UserBO getAuthor() {
        return author;
    }

    public void setAuthor(UserBO author) {
        this.author = author;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }
}
