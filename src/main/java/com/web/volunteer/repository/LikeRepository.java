package com.web.volunteer.repository;

import com.web.volunteer.entity.Comment;
import com.web.volunteer.entity.Like;
import com.web.volunteer.entity.Post;
import com.web.volunteer.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LikeRepository extends JpaRepository<Like, Long> {

    Optional<Like> findByUserAndPost(User user, Post post);

    Optional<Like> findByUserAndComment(User user, Comment comment);

    boolean existsByUserAndPost(User user, Post post);

    boolean existsByUserAndComment(User user, Comment comment);

    long countByPost(Post post);

    long countByComment(Comment comment);

    void deleteByUserAndPost(User user, Post post);

    void deleteByUserAndComment(User user, Comment comment);
}
