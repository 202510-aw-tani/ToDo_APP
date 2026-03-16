package com.example.todo.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.example.todo.model.Todo;

public interface TodoMapper {
    List<Todo> findAll();

    List<Todo> findAllByUserId(@Param("userId") Long userId);

    List<Todo> findPage(@Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sortByPriority") boolean sortByPriority,
            @Param("sortByDeadline") boolean sortByDeadline,
            @Param("categoryId") Long categoryId,
            @Param("userId") Long userId);

    List<Todo> findPageForAdmin(@Param("limit") int limit,
            @Param("offset") int offset,
            @Param("sortByPriority") boolean sortByPriority,
            @Param("sortByDeadline") boolean sortByDeadline,
            @Param("categoryId") Long categoryId);

    long countAll(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    long countAllForAdmin(@Param("categoryId") Long categoryId);

    int insert(Todo todo);

    int deleteById(@Param("id") Long id);

    int deleteByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int deleteByIds(@Param("ids") List<Integer> ids);

    int deleteByIdsAndUserId(@Param("ids") List<Integer> ids, @Param("userId") Long userId);

    Todo findById(@Param("id") Long id);

    Todo findByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int existsById(@Param("id") Long id);

    int countByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    int updateById(Todo todo);

    int updateByIdAndUserId(Todo todo);

    int assignUnownedTodosToUser(@Param("userId") Long userId);
}
