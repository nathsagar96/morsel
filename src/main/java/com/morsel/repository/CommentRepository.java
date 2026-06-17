package com.morsel.repository;

import com.morsel.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "recipe"})
    List<Comment> findByRecipeIdOrderByCreatedAtDesc(Long recipeId);
}
