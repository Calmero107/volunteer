package com.web.volunteer.repository;

import com.web.volunteer.entity.Comment;
import com.web.volunteer.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByPost(Post post, Pageable pageable);

    Page<Comment> findByPostOrderByCreatedAtDesc(Post post, Pageable pageable);
}
