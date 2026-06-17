package com.morsel.repository;

import com.morsel.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "recipe"})
    Page<Comment> findByRecipeIdOrderByCreatedAtDesc(Long recipeId, Pageable pageable);
}
